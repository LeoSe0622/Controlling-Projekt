package de.leo.controlling;

import de.leo.controlling.io.CsvEinleser;
import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.report.Berichtsmodell;
import de.leo.controlling.report.BerichtsmodellBauer;
import de.leo.controlling.report.ExcelReportWriter;
import de.leo.controlling.report.KonsolenReport;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Einstiegspunkt. Verdrahtet die Pipeline und sonst nichts:
 * Lesen -> Modell bauen -> Ausgeben.
 *
 * <p>Alles Fachliche liegt im {@code BerichtsmodellBauer}, alles Darstellende im
 * {@code KonsolenReport}. Diese Klasse trifft nur zwei Entscheidungen, die wirklich
 * hierher gehoeren: welche Datei gelesen wird und mit welchem Exit-Code das Programm
 * endet.
 *
 * <p>Exit-Codes, damit das Programm automatisierbar bleibt:
 * 0 = sauber, 1 = nur Warnungen, 2 = Fehlerzeilen vorhanden, 3 = Datei unlesbar.
 */
public class App {

    public static void main(String[] args) {

        // pfad steht bewusst VOR dem try: der catch-Block braucht ihn noch fuer die Meldung.
        Path pfad = Path.of("controlling_rohdaten.csv");

        try {
            List<Rohzeile> roh = new CsvEinleser().lies(pfad);

            // LocalDateTime.now() steht HIER, nicht im Bauer: Nur so bleibt der Bauer
            // testbar - Tests uebergeben einen festen Zeitpunkt.
            Berichtsmodell modell = new BerichtsmodellBauer()
                    .baue(pfad.getFileName().toString(), roh, LocalDateTime.now());

            new KonsolenReport().schreibe(modell);

            // Der Excel-Bericht landet neben der Eingabedatei, nicht im
            // Arbeitsverzeichnis: Beim Reinziehen der CSV (Phase 8) waere das
            // sonst irgendein Windows-Systemordner, und man sucht die Datei.
            Path ziel = ausgabepfadFuer(pfad);
            new ExcelReportWriter().schreibe(modell, ziel);
            System.out.println();
            System.out.println("Bericht geschrieben: " + ziel.toAbsolutePath());

            if (modell.protokoll().anzahlFehler() > 0) {
                System.exit(2);
            } else if (modell.protokoll().anzahlWarnungen() > 0) {
                System.exit(1);
            }

        } catch (IOException e) {
            System.err.println("Datei konnte nicht gelesen oder geschrieben werden: "
                    + pfad.toAbsolutePath());
            System.err.println("Grund: " + e.getMessage());
            System.exit(3);
        }
    }

    /**
     * Leitet den Ausgabepfad aus der Eingabedatei ab:
     * {@code daten/maerz.csv} wird zu {@code daten/maerz_Monatsbericht.xlsx}.
     */
    private static Path ausgabepfadFuer(Path eingabe) {
        String name = eingabe.getFileName().toString();
        int punkt = name.lastIndexOf('.');
        String ohneEndung = punkt > 0 ? name.substring(0, punkt) : name;

        Path ordner = eingabe.toAbsolutePath().getParent();
        return ordner.resolve(ohneEndung + "_Monatsbericht.xlsx");
    }
}
