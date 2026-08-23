package de.leo.controlling.report;

import de.leo.controlling.io.CsvEinleser;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueft die geschriebene Excel-Datei, indem sie wieder eingelesen wird.
 *
 * <p>Das ist der einzige ehrliche Weg, eine Dateiausgabe zu testen: Nicht pruefen, ob
 * die Schreibmethode aufgerufen wurde, sondern ob am Ende eine Datei da ist, die sich
 * oeffnen laesst und die richtigen Werte enthaelt.
 *
 * <p>{@code @TempDir} legt fuer jeden Test ein eigenes Verzeichnis an und raeumt es
 * hinterher weg - so bleiben keine Testdateien im Projekt liegen.
 */
class ExcelReportWriterTest {

    private static final LocalDateTime STICHTAG = LocalDateTime.of(2025, 12, 31, 23, 59);

    @TempDir
    Path tempDir;

    private static Berichtsmodell modell() throws IOException {
        return new BerichtsmodellBauer().baue(
                "controlling_rohdaten.csv",
                new CsvEinleser().lies(Path.of("controlling_rohdaten.csv")),
                STICHTAG);
    }

    @Test
    void schreibtEineLesbareDateiMitAllenTabs() throws IOException {
        Path ziel = tempDir.resolve("Monatsbericht.xlsx");

        new ExcelReportWriter().schreibe(modell(), ziel);

        assertTrue(Files.exists(ziel), "Datei wurde nicht angelegt");
        assertTrue(Files.size(ziel) > 0, "Datei ist leer");

        // Wieder oeffnen - gelingt das nicht, ist die Datei kaputt.
        try (Workbook wb = WorkbookFactory.create(ziel.toFile())) {
            assertEquals(8, wb.getNumberOfSheets());
            assertNotNull(wb.getSheet("Abweichungsbruecke"));
            assertNotNull(wb.getSheet("Zeitreihe"));
            assertNotNull(wb.getSheet("Kostenstellen"));
            assertNotNull(wb.getSheet("Rohdaten"));
            assertNotNull(wb.getSheet("Deckblatt"));
            assertNotNull(wb.getSheet("Datenqualitaet"));
            assertNotNull(wb.getSheet("DB-Rechnung"));
            assertNotNull(wb.getSheet("Abweichungsanalyse"));
        }
    }

    @Test
    void dbRechnungHatEineZeileJeProduktPlusSumme() throws IOException {
        Path ziel = tempDir.resolve("Monatsbericht.xlsx");
        new ExcelReportWriter().schreibe(modell(), ziel);

        try (Workbook wb = WorkbookFactory.create(ziel.toFile())) {
            Sheet blatt = wb.getSheet("DB-Rechnung");

            // Kopfzeile + 4 Produkte + Summenzeile = 6 Zeilen, letzter Index 5.
            assertEquals(5, blatt.getLastRowNum());
            assertEquals("Produkt A", blatt.getRow(1).getCell(0).getStringCellValue());
            assertEquals("GESAMT", blatt.getRow(5).getCell(0).getStringCellValue());
        }
    }

    @Test
    void abweichungsTabZeigtDieZerlegung() throws IOException {
        Path ziel = tempDir.resolve("Monatsbericht.xlsx");
        new ExcelReportWriter().schreibe(modell(), ziel);

        try (Workbook wb = WorkbookFactory.create(ziel.toFile())) {
            Sheet blatt = wb.getSheet("Abweichungsanalyse");

            // Produkt A: Preisabweichung +2.660,00
            assertEquals(2660.00, blatt.getRow(1).getCell(1).getNumericCellValue(), 0.005);
            // Produkt B: Gesamtabweichung -16.175,64
            assertEquals(-16175.64, blatt.getRow(2).getCell(5).getNumericCellValue(), 0.005);

            assertTrue(enthaeltText(blatt, "Abstimmbruecke"), "Brueckenhinweis fehlt");
        }
    }

    @Test
    void datenqualitaetsTabEnthaeltAlleBefunde() throws IOException {
        Path ziel = tempDir.resolve("Monatsbericht.xlsx");
        new ExcelReportWriter().schreibe(modell(), ziel);

        try (Workbook wb = WorkbookFactory.create(ziel.toFile())) {
            Sheet blatt = wb.getSheet("Datenqualitaet");

            // Kopfzeile plus sieben Befunde. getLastRowNum() ist NULL-basiert:
            // bei acht Zeilen liefert es 7.
            assertEquals(7, blatt.getLastRowNum(),
                    "erwartet: Kopfzeile + 7 Befunde");

            // Erste Datenzeile ist Befund 1: Zeile 14, V05, istPreis
            assertEquals(14, (int) blatt.getRow(1).getCell(0).getNumericCellValue());
            assertEquals("istPreis", blatt.getRow(1).getCell(1).getStringCellValue());
            assertEquals("V05", blatt.getRow(1).getCell(2).getStringCellValue());
        }
    }

    @Test
    void deckblattZeigtBetriebsergebnisUndBruecke() throws IOException {
        Path ziel = tempDir.resolve("Monatsbericht.xlsx");
        new ExcelReportWriter().schreibe(modell(), ziel);

        try (Workbook wb = WorkbookFactory.create(ziel.toFile())) {
            Sheet blatt = wb.getSheet("Deckblatt");

            // Irgendwo auf dem Blatt muessen 753.000,00 (Plan) und 1.483.331,65 (Ist)
            // als ZAHL stehen - nicht als Text, sonst kann Excel nicht damit rechnen.
            assertTrue(enthaeltZahl(blatt, 753000.00), "Plan-Betriebsergebnis fehlt");
            assertTrue(enthaeltZahl(blatt, 1483331.65), "Ist-Betriebsergebnis fehlt");

            assertTrue(enthaeltText(blatt, "Abstimmbruecke"), "Brueckenhinweis fehlt");
        }
    }

    private static boolean enthaeltZahl(Sheet blatt, double gesucht) {
        for (var row : blatt) {
            for (var cell : row) {
                if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                        && Math.abs(cell.getNumericCellValue() - gesucht) < 0.005) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean enthaeltText(Sheet blatt, String teil) {
        for (var row : blatt) {
            for (var cell : row) {
                if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING
                        && cell.getStringCellValue().contains(teil)) {
                    return true;
                }
            }
        }
        return false;
    }
}
