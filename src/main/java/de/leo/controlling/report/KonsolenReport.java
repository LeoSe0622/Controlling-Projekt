package de.leo.controlling.report;

import de.leo.controlling.abweichung.Abweichung;
import de.leo.controlling.abweichung.Ampel;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.rechnung.Produktergebnis;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Gibt ein {@link Berichtsmodell} auf der Konsole aus.
 *
 * <p>Diese Klasse rechnet nichts. Sie entscheidet nur ueber Spaltenbreiten, Reihenfolge
 * und Formatierung — alles Fragen der Darstellung. Was WAHR ist, steht im Modell; wie es
 * AUSSIEHT, steht hier.
 *
 * <p>Der ExcelReportWriter wird dieselben Getter aufrufen und andere Entscheidungen
 * treffen. Dass beide dasselbe Modell lesen, ist der Grund, warum sie nicht auseinander
 * laufen koennen.
 *
 * <p>Eine Methode je Abschnitt: Wer die Reihenfolge im Bericht aendern will, sortiert die
 * Aufrufe in {@link #schreibe}, statt in einer 150-Zeilen-Methode zu suchen.
 */
public final class KonsolenReport {

    private static final DateTimeFormatter ZEITSTEMPEL =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public void schreibe(Berichtsmodell m) {
        kopf(m);
        datenqualitaet(m);
        deckungsbeitraege(m);
        abweichungen(m);
        abstimmbruecke(m);
    }

    private void kopf(Berichtsmodell m) {
        System.out.println("Eingelesen: " + m.alleZeilen().size()
                + " Rohzeilen aus " + m.quelle());
        System.out.println("Erstellt:   " + m.erstelltAm().format(ZEITSTEMPEL));
        System.out.println();
    }

    private void datenqualitaet(Berichtsmodell m) {
        for (Befund b : m.protokoll().befunde()) {
            System.out.printf("  Zeile %-3d  %-8s  %-22s  %s%n",
                    b.zeilennummer(), b.regelId(), b.feld(), b.meldung());
        }

        System.out.println();
        System.out.println("Befunde gesamt:     " + m.protokoll().befunde().size());
        System.out.println("davon Fehler:       " + m.protokoll().anzahlFehler());
        System.out.println("davon Warnungen:    " + m.protokoll().anzahlWarnungen());
        System.out.println("verwertbare Zeilen: " + m.protokoll().verwertbareZeilen().size()
                + " von " + m.protokoll().gepruefteZeilen());
        System.out.printf("Qualitaetsquote:    %.1f %%%n",
                m.protokoll().qualitaetsquote() * 100);
        System.out.println();
    }

    private void deckungsbeitraege(Berichtsmodell m) {
        System.out.printf("%-12s %8s %15s %15s %15s%n",
                "Produkt", "Monate", "Plan-DB II", "Ist-DB II", "Abweichung");
        System.out.println("-".repeat(70));

        for (Produktergebnis e : m.produkte()) {
            System.out.printf("%-12s %8s %15s %15s %15s%n",
                    e.produkt(),
                    e.monate() + "/" + m.erwarteteMonate(),
                    geld(e.plan().dbZwei()),
                    geld(e.ist().dbZwei()),
                    geld(e.dbZweiAbweichung()));
        }

        System.out.println("-".repeat(70));
        System.out.printf("%-12s %8s %15s %15s %15s%n",
                "GESAMT", "",
                geld(m.gesamtPlan().dbZwei()),
                geld(m.gesamtIst().dbZwei()),
                geld(m.gesamtIst().dbZwei().subtract(m.gesamtPlan().dbZwei())));

        if (!m.unvollstaendigeProdukte().isEmpty()) {
            System.out.println();
            for (Produktergebnis e : m.unvollstaendigeProdukte()) {
                System.out.printf(
                        "Hinweis: %s umfasst nur %d von %d Monaten - Jahreswerte nur "
                                + "eingeschraenkt vergleichbar.%n",
                        e.produkt(), e.monate(), m.erwarteteMonate());
            }
        }
    }

    private void abweichungen(Berichtsmodell m) {
        System.out.println();
        System.out.printf("%-12s %15s %15s %15s %15s %6s%n",
                "Produkt", "Preis", "Menge", "Misch", "Gesamt", "Ampel");
        System.out.println("-".repeat(83));

        for (Produktergebnis e : m.produkte()) {
            Abweichung a = m.abweichungen().get(e.produkt());

            System.out.printf("%-12s %15s %15s %15s %15s %6s%n",
                    e.produkt(),
                    geld(a.preisabweichung()),
                    geld(a.mengenabweichung()),
                    geld(a.mischabweichung()),
                    geld(a.gesamt()),
                    ampelText(m.ampelFuer(e)));
        }

        Abweichung gesamt = m.gesamtAbweichung();
        System.out.println("-".repeat(83));
        System.out.printf("%-12s %15s %15s %15s %15s %6s%n", "GESAMT",
                geld(gesamt.preisabweichung()),
                geld(gesamt.mengenabweichung()),
                geld(gesamt.mischabweichung()),
                geld(gesamt.gesamt()),
                "");
        System.out.println();
    }

    private void abstimmbruecke(Berichtsmodell m) {
        if (m.brueckeGehtAuf()) {
            System.out.println("Abstimmbruecke: geht auf.");
        } else {
            System.out.println("ACHTUNG - Abstimmbruecke geht NICHT auf. Differenz: "
                    + geld(m.brueckenDifferenz()));
        }
    }

    /** Geldbetrag mit Tausenderpunkt und zwei Nachkommastellen. */
    private static String geld(BigDecimal betrag) {
        return String.format("%,.2f", betrag);
    }

    /**
     * Textdarstellung der Ampel.
     *
     * <p>Bewusst keine Emoji: Die Windows-Konsole stellt sie je nach Codepage als
     * Kaestchen dar. Im Excel wird daraus echte Zellenfarbe.
     */
    private static String ampelText(Ampel ampel) {
        return switch (ampel) {
            case GRUEN -> "[ok]";
            case GELB -> "[!]";
            case ROT -> "[!!]";
        };
    }
}
