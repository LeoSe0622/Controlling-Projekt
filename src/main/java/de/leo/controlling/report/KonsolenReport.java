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
 * und Formatierung - alles Fragen der Darstellung. Was WAHR ist, steht im Modell; wie es
 * AUSSIEHT, steht hier.
 *
 * <p>Der ExcelReportWriter ruft dieselben Getter auf und trifft andere Entscheidungen.
 * Dass beide dasselbe Modell lesen, ist der Grund, warum sie nicht auseinander laufen
 * koennen.
 *
 * <p>Eine Methode je Abschnitt: Wer die Reihenfolge im Bericht aendern will, sortiert die
 * Aufrufe in {@link #schreibe}, statt in einer 150-Zeilen-Methode zu suchen.
 */
public final class KonsolenReport {

    private static final DateTimeFormatter ZEITSTEMPEL =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /** Untergrenze der Produktspalte, damit die Ueberschrift nicht klemmt. */
    private static final int MINDESTBREITE_PRODUKT = 12;

    public void schreibe(Berichtsmodell m) {
        kopf(m);
        datenqualitaet(m);
        deckungsbeitraege(m);
        abweichungen(m);
        warnzeileneinfluss(m);
        abstimmbruecke(m);
    }

    /** Herkunft der Zahlen: Ohne Quelle und Zeitstempel ist ein Bericht nicht pruefbar. */
    private void kopf(Berichtsmodell m) {
        System.out.println("Eingelesen: " + m.alleZeilen().size()
                + " Rohzeilen aus " + m.quelle());
        System.out.println("Erstellt:   " + m.erstelltAm().format(ZEITSTEMPEL));
        System.out.println();
    }

    /** Alle Befunde, danach die Kennzahlen zur Datenqualitaet. */
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

    /**
     * Tabelle mit DB II je Produkt, Summenzeile und Hinweisen zu fehlenden Monaten.
     *
     * <p>Die Breite der Produktspalte richtet sich nach dem laengsten Namen. Eine feste
     * Breite bricht, sobald ein Name laenger ist: {@code printf} kuerzt nicht, sondern
     * schiebt alles Folgende nach rechts - dann fluchten die Zahlenspalten nicht mehr
     * und die Tabelle ist unlesbar.
     */
    private void deckungsbeitraege(Berichtsmodell m) {
        int breite = produktspaltenbreite(m);
        String zeilenformat = "%-" + breite + "s %8s %15s %15s %15s%n";
        int strichbreite = breite + 3 * 16 + 9;

        System.out.printf(zeilenformat,
                "Produkt", "Monate", "Plan-DB II", "Ist-DB II", "Abweichung");
        System.out.println("-".repeat(strichbreite));

        for (Produktergebnis e : m.produkte()) {
            System.out.printf(zeilenformat,
                    e.produkt(),
                    e.monate() + "/" + m.erwarteteMonate(),
                    geld(e.plan().dbZwei()),
                    geld(e.ist().dbZwei()),
                    geld(e.dbZweiAbweichung()));
        }

        System.out.println("-".repeat(strichbreite));
        System.out.printf(zeilenformat,
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

    /**
     * Tabelle mit der Zerlegung der Abweichung je Produkt.
     *
     * <p>Die Ampel der Summenzeile bleibt leer: Ueber alle Produkte heben sich
     * gegenlaeufige Effekte auf, und eine Ampel darauf wuerde genau das verschleiern,
     * was die Zeilen darueber zeigen.
     */
    private void abweichungen(Berichtsmodell m) {
        int breite = produktspaltenbreite(m);
        String zeilenformat = "%-" + breite + "s %15s %15s %15s %15s %6s%n";
        int strichbreite = breite + 4 * 16 + 7 + 5;

        System.out.println();
        System.out.printf(zeilenformat,
                "Produkt", "Preis", "Menge", "Misch", "Gesamt", "Ampel");
        System.out.println("-".repeat(strichbreite));

        for (Produktergebnis e : m.produkte()) {
            Abweichung a = m.abweichungen().get(e.produkt());

            System.out.printf(zeilenformat,
                    e.produkt(),
                    geld(a.preisabweichung()),
                    geld(a.mengenabweichung()),
                    geld(a.mischabweichung()),
                    geld(a.gesamt()),
                    ampelText(m.ampelFuer(e)));
        }

        Abweichung gesamt = m.gesamtAbweichung();
        System.out.println("-".repeat(strichbreite));
        System.out.printf(zeilenformat, "GESAMT",
                geld(gesamt.preisabweichung()),
                geld(gesamt.mengenabweichung()),
                geld(gesamt.mischabweichung()),
                geld(gesamt.gesamt()),
                "");
        System.out.println();
    }

    /**
     * Wie viel der Abweichung aus beanstandeten Zeilen stammt.
     *
     * <p>Steht direkt unter der Summenzeile, weil genau dort die Zahl steht, die dadurch
     * fragwuerdig wird. Wer die Tabelle liest und aufhoert, hat die wichtigste
     * Einschraenkung dann trotzdem gesehen.
     */
    private void warnzeileneinfluss(Berichtsmodell m) {
        Warnzeileneinfluss w = m.warnzeilen();
        if (w.zeilen() == 0) {
            return;
        }

        System.out.printf("Aus %d beanstandeten Zeilen stammen davon: %s (%s)%n",
                w.zeilen(), geld(w.abweichung()), anteilText(m));
        System.out.printf("Abweichung ohne diese Zeilen:            %s%n",
                geld(m.abweichungOhneWarnzeilen()));

        if (m.warnzeilenKippenDasErgebnis()) {
            System.out.println();
            System.out.println("ACHTUNG - ohne die beanstandeten Zeilen dreht sich das "
                    + "Vorzeichen des Ergebnisses um.");
            System.out.println("Die Zahlen oben sind richtig gerechnet, als Kernaussage "
                    + "aber nicht belastbar.");
        }
        System.out.println();
    }

    /** Meldet, ob die Zerlegung und die Deckungsbeitraege dasselbe Ergebnis liefern. */
    private void abstimmbruecke(Berichtsmodell m) {
        if (m.brueckeGehtAuf()) {
            System.out.println("Abstimmbruecke: geht auf.");
        } else {
            System.out.println("ACHTUNG - Abstimmbruecke geht NICHT auf. Differenz: "
                    + geld(m.brueckenDifferenz()));
        }
    }

    /**
     * Breite der Produktspalte: der laengste Produktname, mindestens aber so breit,
     * dass die Ueberschriften "Produkt" und "GESAMT" hineinpassen.
     */
    private static int produktspaltenbreite(Berichtsmodell m) {
        int breite = MINDESTBREITE_PRODUKT;
        for (Produktergebnis e : m.produkte()) {
            breite = Math.max(breite, e.produkt().length());
        }
        return breite;
    }

    /** Geldbetrag mit Tausenderpunkt und zwei Nachkommastellen. */
    private static String geld(BigDecimal betrag) {
        return String.format("%,.2f", betrag);
    }

    /** Der Anteil an der Gesamtabweichung - oder ein Strich, wenn es keine gibt. */
    private static String anteilText(Berichtsmodell m) {
        BigDecimal anteil = m.anteilDerWarnzeilen();
        if (anteil == null) {
            return "Gesamtabweichung ist null";
        }
        return String.format("%,.1f %% der Gesamtabweichung",
                anteil.multiply(new BigDecimal("100")));
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
