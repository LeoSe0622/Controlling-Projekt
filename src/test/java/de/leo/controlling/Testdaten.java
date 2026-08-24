package de.leo.controlling;

import de.leo.controlling.io.CsvEinleser;
import de.leo.controlling.model.Datenzeile;
import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Validator;
import de.leo.controlling.report.Berichtsmodell;
import de.leo.controlling.report.BerichtsmodellBauer;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Zugriff auf die feste Testdatei unter {@code src/test/resources/testdaten.csv}.
 *
 * <p><b>Warum nicht die echte {@code controlling_rohdaten.csv}:</b> Tests sollen Aussagen
 * ueber den CODE treffen, nicht ueber die gerade vorliegende Datendatei. Wer
 * {@code assertEquals(4, produkte.size())} gegen eine Arbeitsdatei schreibt, hat eine
 * Eigenschaft der DATEI festgeschrieben — und die aendert sich, sobald jemand neue Daten
 * erzeugt. Dann melden die Tests einen Fehler, obwohl nichts kaputt ist, und nach dem
 * dritten Fehlalarm schaut niemand mehr hin.
 *
 * <p>Diese Datei aendert sich nie. Sie enthaelt bewusst alle vier Fehlerarten:
 *
 * <pre>
 *   Zeile  5 + 18   Dublette (2025-04, Produkt A, Vertrieb Nord)   V07  FEHLER
 *   Zeile  9        leeres istMenge                               V02  FEHLER
 *   Zeile 17        Ausreisser 17950 statt 350                    V08  WARNUNG
 *   Zeile 19        negativer istPreis                            V05  FEHLER
 * </pre>
 *
 * <p>Daraus folgt: 18 Rohzeilen, 5 Befunde, 4 Fehlerzeilen, 14 verwertbare Zeilen,
 * Qualitaetsquote 77,8 %.
 *
 * <p>V09 (Zukunftsmonat) schweigt hier: Alle Monate liegen im ersten Quartal 2025 und
 * damit vor dem {@link #STICHTAG}. Wer den Stichtag zurueckdatiert, loest zusaetzliche
 * Warnungen aus und bringt die Zahlen oben zum Kippen.
 */
public final class Testdaten {

    /** Fester Zeitstempel — ein Modell, das sich die Uhrzeit selbst holt, ist nicht testbar. */
    public static final LocalDateTime STICHTAG = LocalDateTime.of(2025, 12, 31, 23, 59);

    private static final Path PFAD = Path.of("src", "test", "resources", "testdaten.csv");

    private Testdaten() {
    }

    /** Die 18 Rohzeilen der Testdatei, ungeprueft. */
    public static List<Rohzeile> rohzeilen() throws IOException {
        return new CsvEinleser().lies(PFAD);
    }

    /** Die 14 verwertbaren Zeilen als Datenzeile — validiert und geparst. */
    public static List<Datenzeile> datenzeilen() throws IOException {
        return new Validator().pruefe(rohzeilen())
                .verwertbareZeilen().stream()
                .map(Datenzeile::aus)
                .toList();
    }

    /** Das fertige Berichtsmodell mit festem Zeitstempel und Standardschwellen. */
    public static Berichtsmodell modell() throws IOException {
        return new BerichtsmodellBauer().baue("testdaten.csv", rohzeilen(), STICHTAG);
    }
}
