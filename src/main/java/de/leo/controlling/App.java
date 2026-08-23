package de.leo.controlling;

import de.leo.controlling.config.Berichtskonfiguration;
import de.leo.controlling.io.CsvEinleser;
import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.report.Berichtsmodell;
import de.leo.controlling.report.BerichtsmodellBauer;
import de.leo.controlling.report.ExcelReportWriter;
import de.leo.controlling.report.KonsolenReport;

import java.awt.Desktop;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Einstiegspunkt. Verdrahtet die Pipeline und sonst nichts:
 * Argumente lesen -> Daten lesen -> Modell bauen -> ausgeben.
 *
 * <p>Diese Klasse trifft keine fachliche Entscheidung mehr. Welche Datei gelesen wird,
 * steht in der {@link Berichtskonfiguration}; was gerechnet wird, im BerichtsmodellBauer;
 * wie es aussieht, in den beiden Report-Klassen.
 *
 * <p>Exit-Codes, damit das Programm automatisierbar bleibt:
 * 0 = sauber, 1 = nur Warnungen, 2 = Fehlerzeilen vorhanden,
 * 3 = Abbruch (Argumente falsch oder Datei unlesbar).
 *
 * <p>Der Aufruf zum Oeffnen steht VOR den {@code System.exit}-Zeilen: Bei Exit-Code 1
 * oder 2 wuerde er sonst nie ausgefuehrt, und das sind die haeufigsten Faelle. Im
 * {@code catch}-Block hat er nichts zu suchen - bei Code 3 gibt es keinen Bericht.
 *
 * <p>{@code isDesktopSupported()} ist Pflicht: Auf einem Server ohne grafische
 * Oberflaeche fliegt sonst eine HeadlessException, ausgerechnet dort, wo niemand
 * sie sieht.
 *
 * <p>Das {@code return} nach {@code System.exit(3)} erreicht die JVM nie. Der Compiler
 * weiss das aber nicht und wuerde sonst "konfig ist vielleicht nicht initialisiert"
 * melden.
 */
public class App {

    public static void main(String[] args) {

        Berichtskonfiguration konfig;
        try {
            konfig = Berichtskonfiguration.ausArgumenten(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Fehler: " + e.getMessage());
            System.err.println();
            System.err.println(Berichtskonfiguration.HILFE);
            System.exit(3);
            return;
        }

        try {
            List<Rohzeile> roh = new CsvEinleser().lies(konfig.eingabe());

            Berichtsmodell modell = new BerichtsmodellBauer().baue(
                    konfig.eingabe().getFileName().toString(),
                    roh,
                    LocalDateTime.now(),
                    konfig.wesentlichkeit());

            new KonsolenReport().schreibe(modell);

            new ExcelReportWriter().schreibe(modell, konfig.ausgabe());
            System.out.println();
            System.out.println("Bericht geschrieben: " + konfig.ausgabe());

            if (konfig.oeffnen() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(konfig.ausgabe().toFile());
            }
            if (modell.protokoll().anzahlFehler() > 0) {
                System.exit(2);
            } else if (modell.protokoll().anzahlWarnungen() > 0) {
                System.exit(1);
            }

        } catch (IOException e) {
            System.err.println("Datei konnte nicht gelesen oder geschrieben werden: "
                    + konfig.eingabe().toAbsolutePath());
            System.err.println("Grund: " + e.getMessage());
            System.exit(3);
        }
    }
}
