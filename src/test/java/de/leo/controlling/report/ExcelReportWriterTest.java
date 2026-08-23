package de.leo.controlling.report;

import de.leo.controlling.Testdaten;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueft die geschriebene Excel-Datei, indem sie wieder eingelesen wird.
 *
 * <p>Das ist der einzige ehrliche Weg, eine Dateiausgabe zu testen: nicht pruefen, ob eine
 * Methode aufgerufen wurde, sondern ob am Ende eine Datei da ist, die sich oeffnen laesst
 * und die richtigen Werte enthaelt.
 *
 * <p>{@code @TempDir} legt fuer jeden Test ein eigenes Verzeichnis an und raeumt es
 * hinterher weg, sodass keine Testdateien im Projekt liegen bleiben.
 */
class ExcelReportWriterTest {

    @TempDir
    Path tempDir;

    private Path schreibeBericht() throws IOException {
        Path ziel = tempDir.resolve("Monatsbericht.xlsx");
        new ExcelReportWriter().schreibe(Testdaten.modell(), ziel);
        return ziel;
    }

    @Test
    void schreibtEineLesbareDateiMitAllenTabs() throws IOException {
        Path ziel = schreibeBericht();

        assertTrue(Files.exists(ziel), "Datei wurde nicht angelegt");
        assertTrue(Files.size(ziel) > 0, "Datei ist leer");

        try (Workbook wb = WorkbookFactory.create(ziel.toFile())) {
            assertEquals(8, wb.getNumberOfSheets());
            for (String tab : new String[]{"Deckblatt", "Datenqualitaet", "DB-Rechnung",
                    "Abweichungsanalyse", "Abweichungsbruecke", "Zeitreihe",
                    "Kostenstellen", "Rohdaten"}) {
                assertNotNull(wb.getSheet(tab), "Tab fehlt: " + tab);
            }
        }
    }

    @Test
    void datenqualitaetsTabEnthaeltAlleBefunde() throws IOException {
        try (Workbook wb = WorkbookFactory.create(schreibeBericht().toFile())) {
            Sheet blatt = wb.getSheet("Datenqualitaet");

            // Kopfzeile plus fuenf Befunde. getLastRowNum() ist null-basiert.
            assertEquals(5, blatt.getLastRowNum(), "Kopfzeile + 5 Befunde");

            assertEquals(9, (int) blatt.getRow(1).getCell(0).getNumericCellValue());
            assertEquals("istMenge", blatt.getRow(1).getCell(1).getStringCellValue());
            assertEquals("V02", blatt.getRow(1).getCell(2).getStringCellValue());
        }
    }

    @Test
    void deckblattZeigtBetriebsergebnisUndBruecke() throws IOException {
        try (Workbook wb = WorkbookFactory.create(schreibeBericht().toFile())) {
            Sheet blatt = wb.getSheet("Deckblatt");

            // Als ZAHL, nicht als Text - sonst kann Excel nicht damit rechnen.
            assertTrue(enthaeltZahl(blatt, 249000.00), "Plan-Betriebsergebnis fehlt");
            assertTrue(enthaeltZahl(blatt, 968301.89), "Ist-Betriebsergebnis fehlt");
            assertTrue(enthaeltText(blatt, "Abstimmbruecke"), "Brueckenhinweis fehlt");
        }
    }

    @Test
    void dbRechnungHatEineZeileJeProduktPlusSumme() throws IOException {
        try (Workbook wb = WorkbookFactory.create(schreibeBericht().toFile())) {
            Sheet blatt = wb.getSheet("DB-Rechnung");

            assertEquals(5, blatt.getLastRowNum(), "Kopfzeile + 4 Produkte + Summe");
            assertEquals("Produkt A", blatt.getRow(1).getCell(0).getStringCellValue());
            assertEquals("GESAMT", blatt.getRow(5).getCell(0).getStringCellValue());
        }
    }

    @Test
    void abweichungsTabZeigtDieZerlegung() throws IOException {
        try (Workbook wb = WorkbookFactory.create(schreibeBericht().toFile())) {
            Sheet blatt = wb.getSheet("Abweichungsanalyse");

            assertEquals(480.00, blatt.getRow(1).getCell(1).getNumericCellValue(), 0.005);
            assertEquals(-1198.38, blatt.getRow(1).getCell(5).getNumericCellValue(), 0.005);
            assertTrue(enthaeltText(blatt, "Abstimmbruecke"), "Brueckenhinweis fehlt");
        }
    }

    @Test
    void rohdatenTabZeigtAlleZeilenMitStatus() throws IOException {
        try (Workbook wb = WorkbookFactory.create(schreibeBericht().toFile())) {
            Sheet blatt = wb.getSheet("Rohdaten");

            assertEquals(18, blatt.getLastRowNum(), "Kopfzeile + 18 Rohzeilen");

            assertEquals("OK", statusVonZeile(blatt, 2));
            assertEquals("FEHLER", statusVonZeile(blatt, 9), "leeres istMenge");
            assertEquals("FEHLER", statusVonZeile(blatt, 19), "negativer Preis");
            assertEquals("FEHLER", statusVonZeile(blatt, 5), "Dublette");
            assertEquals("WARNUNG", statusVonZeile(blatt, 17), "Ausreisser");
        }
    }

    /**
     * Sucht die Zeile ueber ihre CSV-Zeilennummer in Spalte 0.
     *
     * <p>Nicht ueber die Position im Blatt: Die verschiebt sich um die Kopfzeile und
     * bei jeder Aenderung der Reihenfolge. Der Test soll pruefen, WAS in der Zeile
     * steht, nicht WO sie steht.
     */
    private static String statusVonZeile(Sheet blatt, int csvZeile) {
        for (var row : blatt) {
            var erste = row.getCell(0);
            if (erste != null && erste.getCellType() == CellType.NUMERIC
                    && (int) erste.getNumericCellValue() == csvZeile) {
                return row.getCell(1).getStringCellValue();
            }
        }
        throw new AssertionError("Zeile " + csvZeile + " fehlt im Rohdaten-Tab");
    }

    private static boolean enthaeltZahl(Sheet blatt, double gesucht) {
        for (var row : blatt) {
            for (var cell : row) {
                if (cell.getCellType() == CellType.NUMERIC
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
                if (cell.getCellType() == CellType.STRING
                        && cell.getStringCellValue().contains(teil)) {
                    return true;
                }
            }
        }
        return false;
    }
}
