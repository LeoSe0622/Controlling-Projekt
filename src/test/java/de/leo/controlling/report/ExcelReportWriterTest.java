package de.leo.controlling.report;

import de.leo.controlling.Testdaten;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    /**
     * Alle Befunde, aufsteigend nach Zeilennummer - ueber beide Regelarten hinweg.
     *
     * <p>Zeile 5 und 18 sind die Dublette und kommen aus einer Datensatzregel, die NACH
     * allen Zeilenregeln laeuft. Ohne die Sortierung im Validator stuenden sie am Ende,
     * der Tab finge in der Mitte wieder von vorn an, und wer nachschlagen will, was mit
     * Zeile 18 los ist, muesste an zwei Stellen suchen.
     */
    @Test
    void datenqualitaetsTabZeigtAlleBefundeNachZeileSortiert() throws IOException {
        try (Workbook wb = WorkbookFactory.create(schreibeBericht().toFile())) {
            Sheet blatt = wb.getSheet("Datenqualitaet");

            // Kopfzeile plus fuenf Befunde. getLastRowNum() ist null-basiert.
            assertEquals(5, blatt.getLastRowNum(), "Kopfzeile + 5 Befunde");
            assertEquals(List.of(5, 9, 17, 18, 19), zeilennummern(blatt));

            Row neun = befundZuZeile(blatt, 9);
            assertEquals("istMenge", neun.getCell(1).getStringCellValue());
            assertEquals("V02", neun.getCell(2).getStringCellValue());
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

    /**
     * Das Deckblatt sagt, wie belastbar sein eigenes Betriebsergebnis ist.
     *
     * <p>Ohne diesen Block meldet die Kopfzahl 719.301,89 EUR ueber Plan, und niemand
     * erfaehrt, dass eine einzige beanstandete Zeile sie traegt. Auf den echten Daten
     * waren es neun Zeilen und 1,4 Mio EUR.
     */
    @Test
    void deckblattZeigtDenEinflussDerBeanstandetenZeilen() throws IOException {
        try (Workbook wb = WorkbookFactory.create(schreibeBericht().toFile())) {
            Sheet blatt = wb.getSheet("Deckblatt");

            assertTrue(enthaeltZahl(blatt, 728143.00),
                    "die Abweichung der beanstandeten Zeilen fehlt");
            assertTrue(enthaeltZahl(blatt, -8841.11),
                    "die Abweichung OHNE diese Zeilen fehlt");
            assertTrue(enthaeltText(blatt, "Vorzeichen"),
                    "der Hinweis auf den Vorzeichenwechsel fehlt");
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

    /**
     * Die Ampel steht als Text in der Zelle, nicht nur als Farbe.
     *
     * <p>Eine gefaerbte, aber leere Zelle traegt ihre Information ausschliesslich im
     * Fuellmuster. Schwarz-weiss gedruckt, fuer einen farbenblinden Leser oder nach
     * "Speichern als CSV" ist sie eine leere Zelle.
     */
    @Test
    void ampelSpalteTraegtAuchText() throws IOException {
        try (Workbook wb = WorkbookFactory.create(schreibeBericht().toFile())) {
            Sheet blatt = wb.getSheet("Abweichungsanalyse");

            assertEquals("beobachten", blatt.getRow(1).getCell(6).getStringCellValue(),
                    "Produkt A ist gelb");
            assertEquals("handeln", blatt.getRow(2).getCell(6).getStringCellValue(),
                    "Produkt B ist rot");
            assertEquals("beobachten", blatt.getRow(3).getCell(6).getStringCellValue());
            assertEquals("beobachten", blatt.getRow(4).getCell(6).getStringCellValue());
        }
    }

    /**
     * Die Zeitreihe fuehrt zwei Summenspalten, und die stimmen mit dem Deckblatt ueberein.
     *
     * <p>Damit ist die Zeitreihe nicht nur bequemer zu lesen, sondern auch eine Probe:
     * Die Monatssummen muessen das Betriebsergebnis ergeben, sonst fehlt irgendwo ein
     * Produkt-Monat.
     */
    @Test
    void zeitreiheHatSummenspaltenDieAufgehen() throws IOException {
        try (Workbook wb = WorkbookFactory.create(schreibeBericht().toFile())) {
            Sheet blatt = wb.getSheet("Zeitreihe");

            assertEquals("Gesamt Plan", blatt.getRow(0).getCell(1).getStringCellValue());
            assertEquals("Gesamt Ist", blatt.getRow(0).getCell(2).getStringCellValue());

            double plan = 0;
            double ist = 0;
            for (int i = 1; i <= blatt.getLastRowNum(); i++) {
                plan += blatt.getRow(i).getCell(1).getNumericCellValue();
                ist += blatt.getRow(i).getCell(2).getNumericCellValue();
            }

            assertEquals(249000.00, plan, 0.005, "Summe der Monate = Plan-Betriebsergebnis");
            assertEquals(968301.89, ist, 0.005, "Summe der Monate = Ist-Betriebsergebnis");
        }
    }

    /**
     * Die Abweichungsspalte ist die Datenquelle des Diagramms - sie muss zu den beiden
     * Spalten links davon passen, sonst zeigt das Deckblatt etwas anderes als die Tabelle.
     */
    @Test
    void zeitreiheHatEineAbweichungsspalteDieZuPlanUndIstPasst() throws IOException {
        try (Workbook wb = WorkbookFactory.create(schreibeBericht().toFile())) {
            Sheet blatt = wb.getSheet("Zeitreihe");

            assertEquals("Gesamt Abweichung", blatt.getRow(0).getCell(3).getStringCellValue());

            double summe = 0;
            for (int i = 1; i <= blatt.getLastRowNum(); i++) {
                Row r = blatt.getRow(i);
                double plan = r.getCell(1).getNumericCellValue();
                double ist = r.getCell(2).getNumericCellValue();
                double abweichung = r.getCell(3).getNumericCellValue();

                assertEquals(ist - plan, abweichung, 0.005,
                        "Monat " + r.getCell(0).getStringCellValue());
                summe += abweichung;
            }

            assertEquals(719301.89, summe, 0.005, "Summe = Gesamtabweichung des Berichts");
        }
    }

    /**
     * Auf dem Deckblatt liegt ein echtes Excel-Diagramm, kein Bild.
     *
     * <p>Geprueft wird, dass es die Datei ueberlebt: geschrieben, wieder eingelesen, und
     * die Serie zeigt auf die Abweichungsspalte der Zeitreihe. Ein Diagramm, das auf den
     * falschen Bereich zeigt, sieht in der Datei plausibel aus und zeigt trotzdem Unsinn.
     */
    @Test
    void deckblattTraegtEinLiniendiagramm() throws IOException {
        try (Workbook wb = WorkbookFactory.create(schreibeBericht().toFile())) {
            XSSFSheet deckblatt = (XSSFSheet) wb.getSheet("Deckblatt");

            List<XSSFChart> diagramme = deckblatt.getDrawingPatriarch().getCharts();
            assertEquals(1, diagramme.size(), "genau ein Diagramm auf dem Deckblatt");

            String xml = diagramme.get(0).getCTChart().toString();

            assertTrue(xml.contains("lineChart"), "es muss ein Liniendiagramm sein");
            assertTrue(xml.contains("Zeitreihe!$D$2:$D$5"),
                    "die Serie muss auf die Abweichungsspalte der Zeitreihe zeigen - "
                            + "vier Monate in der Testdatei, also D2 bis D5");
            assertTrue(xml.contains("Zeitreihe!$A$2:$A$5"),
                    "die Beschriftung muss aus der Monatsspalte kommen");
            assertTrue(xml.contains("crosses val=\"autoZero\""),
                    "ohne kreuzende Nullachse gibt es keine sichtbare Nulllinie");
            assertTrue(xml.contains("smooth val=\"false\""),
                    "geglaettete Linien taeuschen einen Verlauf zwischen Monaten vor");
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
     * Zahlen im Rohdaten-Tab sind Zahlen, kaputte Felder bleiben Text.
     *
     * <p>Alles unbesehen als Text zu schreiben war die urspruengliche Loesung und kostete
     * den Tab seinen Zweck: Excel setzt auf jede dieser Zellen das Warndreieck "Zahl als
     * Text gespeichert", und summieren oder nach Groesse sortieren geht nicht mehr -
     * ausgerechnet dort, wo man eine Zahl aus dem Bericht nachpruefen will.
     */
    @Test
    void rohdatenTabSchreibtZahlenAlsZahlen() throws IOException {
        try (Workbook wb = WorkbookFactory.create(schreibeBericht().toFile())) {
            Sheet blatt = wb.getSheet("Rohdaten");

            Row sauber = zeileMitNummer(blatt, 2);
            assertEquals(CellType.NUMERIC, sauber.getCell(5).getCellType(), "plan_menge");
            assertEquals(1000.0, sauber.getCell(5).getNumericCellValue(), 0.005);
            assertEquals(51.03, sauber.getCell(8).getNumericCellValue(), 0.005, "ist_preis");

            // Zeile 9 hat ein leeres ist_menge - daraus darf keine 0 werden.
            Cell leer = zeileMitNummer(blatt, 9).getCell(6);
            assertNotEquals(CellType.NUMERIC, leer.getCellType(),
                    "ein leeres Feld ist keine Zahl - eine 0 waere eine erfundene Angabe");
        }
    }

    /**
     * Keine Spalte wird unlesbar breit - auf keinem Tab.
     *
     * <p>Der Fall, den dieser Test festhaelt: Ein Hinweissatz von 216 Zeichen stand in
     * Spalte A des Deckblatts und zog sie auf 177 Zeichen auf, weil
     * {@code autoSizeColumn} die LAENGSTE Zelle misst. Das Blatt passte auf keinen
     * Bildschirm mehr, und das Diagramm rutschte aus dem Bild. Ein neuer Hinweis an einer
     * neuen Stelle wuerde denselben Fehler wieder einbauen, ohne dass es auffaellt -
     * Spaltenbreiten sieht man nur, wenn man die Datei oeffnet.
     */
    @Test
    void keineSpalteWirdUnlesbarBreit() throws IOException {
        try (Workbook wb = WorkbookFactory.create(schreibeBericht().toFile())) {
            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                Sheet blatt = wb.getSheetAt(s);

                for (int spalte = 0; spalte < 20; spalte++) {
                    int zeichen = blatt.getColumnWidth(spalte) / 256;

                    assertTrue(zeichen <= 40,
                            blatt.getSheetName() + ", Spalte " + spalte + ": "
                                    + zeichen + " Zeichen breit");
                }
            }
        }
    }

    /** Die Zeilennummern der Befunde in der Reihenfolge, in der sie im Tab stehen. */
    private static List<Integer> zeilennummern(Sheet blatt) {
        List<Integer> nummern = new ArrayList<>();
        for (int i = 1; i <= blatt.getLastRowNum(); i++) {
            nummern.add((int) blatt.getRow(i).getCell(0).getNumericCellValue());
        }
        return nummern;
    }

    private static Row befundZuZeile(Sheet blatt, int csvZeile) {
        return zeileMitNummer(blatt, csvZeile);
    }

    /**
     * Sucht die Zeile ueber ihre CSV-Zeilennummer in Spalte 0.
     *
     * <p>Nicht ueber die Position im Blatt: Die verschiebt sich um die Kopfzeile und
     * bei jeder Aenderung der Reihenfolge. Der Test soll pruefen, WAS in der Zeile
     * steht, nicht WO sie steht.
     */
    private static Row zeileMitNummer(Sheet blatt, int csvZeile) {
        for (Row row : blatt) {
            Cell erste = row.getCell(0);
            if (erste != null && erste.getCellType() == CellType.NUMERIC
                    && (int) erste.getNumericCellValue() == csvZeile) {
                return row;
            }
        }
        throw new AssertionError("Zeile " + csvZeile + " fehlt im Tab");
    }

    private static String statusVonZeile(Sheet blatt, int csvZeile) {
        return zeileMitNummer(blatt, csvZeile).getCell(1).getStringCellValue();
    }

    private static boolean enthaeltZahl(Sheet blatt, double gesucht) {
        for (Row row : blatt) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.NUMERIC
                        && Math.abs(cell.getNumericCellValue() - gesucht) < 0.005) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean enthaeltText(Sheet blatt, String teil) {
        for (Row row : blatt) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING
                        && cell.getStringCellValue().contains(teil)) {
                    return true;
                }
            }
        }
        return false;
    }
}
