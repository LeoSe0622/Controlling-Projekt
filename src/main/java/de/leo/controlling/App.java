package de.leo.controlling;

import de.leo.controlling.io.CsvEinleser;
import de.leo.controlling.model.Datenzeile;
import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Pruefprotokoll;
import de.leo.controlling.pruefung.Validator;
import de.leo.controlling.rechnung.Deckungsbeitrag;
import de.leo.controlling.rechnung.ProduktRechner;
import de.leo.controlling.rechnung.Produktergebnis;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Einstiegspunkt. Verdrahtet die Pipeline:
 * Einlesen -> Validieren -> Rechnen -> [Phase 5: Excel-Report].
 *
 * <p>Exit-Codes, damit das Programm automatisierbar bleibt:
 * 0 = sauber, 1 = nur Warnungen, 2 = Fehlerzeilen vorhanden, 3 = Datei unlesbar.
 */
public class App {

    public static void main(String[] args) {

        // pfad steht bewusst VOR dem try: der catch-Block braucht ihn noch fuer die Meldung.
        Path pfad = Path.of("controlling_rohdaten.csv");

        try {
            CsvEinleser einleser = new CsvEinleser();
            List<Rohzeile> roh = einleser.lies(pfad);

            System.out.println("Eingelesen: " + roh.size() + " Rohzeilen aus " + pfad);
            System.out.println();

            // ---------- Validierung ----------
            Pruefprotokoll protokoll = new Validator().pruefe(roh);

            for (Befund b : protokoll.befunde()) {
                System.out.printf("  Zeile %-3d  %-8s  %-22s  %s%n",
                        b.Zeilennummer(), b.regeId(), b.feld(), b.meldung());
            }

            System.out.println();
            System.out.println("Befunde gesamt:     " + protokoll.befunde().size());
            System.out.println("davon Fehler:       " + protokoll.anzahlFehler());
            System.out.println("davon Warnungen:    " + protokoll.anzahlWarnungen());
            System.out.println("verwertbare Zeilen: " + protokoll.verwertbareZeilen().size()
                    + " von " + protokoll.gepruefteZeilen());
            System.out.printf("Qualitaetsquote:    %.1f %%%n", protokoll.qualitaetsquote() * 100);
            System.out.println();

            // ---------- Deckungsbeitragsrechnung ----------

            List<Datenzeile> daten = protokoll.verwertbareZeilen().stream()
                    .map(Datenzeile::aus)
                    .toList();

            List<Produktergebnis> ergebnisse = new ProduktRechner().jeProdukt(daten);

            // Nicht 12 fest hinschreiben - aus den Daten ableiten.
            int erwarteteMonate = (int) daten.stream()
                    .map(Datenzeile::monat)
                    .distinct()
                    .count();

            System.out.printf("%-12s %8s %15s %15s %15s%n",
                    "Produkt", "Monate", "Plan-DB II", "Ist-DB II", "Abweichung");
            System.out.println("-".repeat(70));

            for (Produktergebnis e : ergebnisse) {
                System.out.printf("%-12s %8s %15s %15s %15s%n",
                        e.produkt(),
                        e.monate() + "/" + erwarteteMonate,
                        geld(e.plan().dbZwei()),
                        geld(e.ist().dbZwei()),
                        geld(e.dbZweiAbweichung()));
            }

            // Summenzeile: Betriebsergebnis ueber alle Produkte.
            // Nur die Geldbetraege - eine summierte Menge ueber verschiedene
            // Produkte hinweg waere bedeutungslos.
            Deckungsbeitrag gesamtPlan = new ProduktRechner().gesamtPlan(ergebnisse);
            Deckungsbeitrag gesamtIst = new ProduktRechner().gesamtIst(ergebnisse);

            System.out.println("-".repeat(70));
            System.out.printf("%-12s %8s %15s %15s %15s%n",
                    "GESAMT", "",
                    geld(gesamtPlan.dbZwei()),
                    geld(gesamtIst.dbZwei()),
                    geld(gesamtIst.dbZwei().subtract(gesamtPlan.dbZwei())));

            // Ein Hinweis JE betroffenem Produkt, direkt unter der Tabelle.
            // Eine Sammelmeldung ("3 Produkte betroffen") schickt den Leser
            // zurueck in die Tabelle, um selbst zu suchen - das ist genau die
            // Arbeit, die der Bericht ihm abnehmen soll.
            List<Produktergebnis> unvollstaendig = ergebnisse.stream()
                    .filter(e -> !e.vollstaendig(erwarteteMonate))
                    .toList();

            if (!unvollstaendig.isEmpty()) {
                System.out.println();
                for (Produktergebnis e : unvollstaendig) {
                    System.out.printf(
                            "Hinweis: %s umfasst nur %d von %d Monaten - Jahreswerte nur "
                                    + "eingeschraenkt vergleichbar.%n",
                            e.produkt(), e.monate(), erwarteteMonate);
                }
            }


            if (protokoll.anzahlFehler() > 0) {
                System.exit(2);
            } else if (protokoll.anzahlWarnungen() > 0) {
                System.exit(1);
            }

        } catch (IOException e) {
            System.err.println("Datei konnte nicht gelesen werden: " + pfad.toAbsolutePath());
            System.err.println("Grund: " + e.getMessage());
            System.exit(3);
        }
    }

    /**
     * Formatiert einen Geldbetrag mit Tausenderpunkt und zwei Nachkommastellen.
     *
     * <p>%,.2f nutzt die Landeseinstellung des Rechners - bei dir also
     * "52.414,70" mit Punkt als Tausendertrenner und Komma als Dezimaltrenner.
     */
    private static String geld(java.math.BigDecimal betrag) {
        return String.format("%,.2f", betrag);
    }
}
