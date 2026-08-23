package de.leo.controlling.io;

import de.leo.controlling.model.Rohzeile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests fuer den CsvEinleser mit ABSICHTLICH kaputten Dateien.
 *
 * <p>Bisher wurde der Einleser nur mit der einen guten CSV benutzt. Seine eigentliche
 * Aufgabe ist aber "stuerze bei keiner Zeile ab" — und genau das war nie geprueft.
 * Hier bekommt er die Faelle, fuer die er gebaut wurde.
 */
class CsvEinleserTest {

    private static final String KOPF =
            "monat,produkt,kostenstelle,plan_menge,ist_menge,plan_preis,ist_preis,"
                    + "variable_stueckkosten,fixkosten_produkt";

    @TempDir
    Path tempDir;

    private List<Rohzeile> lies(String... zeilen) throws IOException {
        Path datei = tempDir.resolve("test.csv");
        Files.writeString(datei, String.join("\n", zeilen), StandardCharsets.UTF_8);
        return new CsvEinleser().lies(datei);
    }

    @Test
    void liestEineNormaleZeile() throws IOException {
        List<Rohzeile> zeilen = lies(KOPF,
                "2025-02,Produkt C,Vertrieb Nord,2000,2233.0,25.0,26.54,12.0,2000");

        assertEquals(1, zeilen.size());

        Rohzeile z = zeilen.get(0);
        assertEquals("2025-02", z.monat());
        assertEquals("Produkt C", z.produkt());
        assertEquals("2233.0", z.istMenge());
        assertEquals("2000", z.fixkostenProdukt());
        assertEquals(9, z.spaltenAnzahl());
    }

    @Test
    void zeilennummerierungBeginntBeiZwei() throws IOException {
        List<Rohzeile> zeilen = lies(KOPF,
                "2025-01,A,Nord,1,1,1,1,1,1",
                "2025-02,B,Nord,1,1,1,1,1,1",
                "2025-03,C,Nord,1,1,1,1,1,1");

        // Die Kopfzeile ist Zeile 1 im Editor, die erste Datenzeile also Zeile 2.
        // Stimmt das nicht, zeigen SAEMTLICHE Fehlermeldungen des Programms
        // auf die falsche Zeile.
        assertEquals(2, zeilen.get(0).zeilennummer());
        assertEquals(3, zeilen.get(1).zeilennummer());
        assertEquals(4, zeilen.get(2).zeilennummer());
    }

    @Test
    void behaeltLeereLetzteSpalte() throws IOException {
        // DER Test fuer das -1 in split(",", -1). Ohne das -1 wirft Java am Ende
        // stehende leere Felder weg: Diese Zeile haette dann 8 statt 9 Spalten,
        // und Regel V01 wuerde faelschlich anschlagen.
        List<Rohzeile> zeilen = lies(KOPF,
                "2025-02,Produkt C,Vertrieb Nord,2000,2233.0,25.0,26.54,12.0,");

        Rohzeile z = zeilen.get(0);
        assertEquals(9, z.spaltenAnzahl(), "die leere letzte Spalte zaehlt mit");
        assertEquals("", z.fixkostenProdukt());
    }

    @Test
    void behaeltLeeresFeldInDerMitte() throws IOException {
        // Das ist der Fall aus Zeile 22 der echten Daten.
        List<Rohzeile> zeilen = lies(KOPF,
                "2025-04,Produkt B,Vertrieb Sued,600,,80.0,83.03,45.0,3000");

        assertEquals("", zeilen.get(0).istMenge());
        assertEquals("80.0", zeilen.get(0).planPreis(), "die Folgespalten duerfen nicht verrutschen");
    }

    @Test
    void stuerztBeiZuWenigSpaltenNichtAb() throws IOException {
        // Eine abgeschnittene Zeile. Ohne die Grenzpruefung in feld() gaebe es hier
        // eine ArrayIndexOutOfBoundsException - und das Programm waere an genau der
        // Sorte Zeile gestorben, fuer die es geschrieben wurde.
        List<Rohzeile> zeilen = lies(KOPF,
                "2025-02,Produkt C,Vertrieb Nord,2000,2233.0,25.0");

        Rohzeile z = zeilen.get(0);
        assertEquals(6, z.spaltenAnzahl(), "die tatsaechliche Spaltenzahl bleibt erhalten");
        assertEquals("25.0", z.planPreis());
        assertEquals("", z.istPreis(), "fehlende Spalten werden mit Leerstring aufgefuellt");
        assertEquals("", z.fixkostenProdukt());
    }

    @Test
    void ignoriertMehrSpaltenAlsErwartet() throws IOException {
        List<Rohzeile> zeilen = lies(KOPF,
                "2025-02,C,Nord,1,1,1,1,1,1,ZUVIEL,NOCHMEHR");

        assertEquals(11, zeilen.get(0).spaltenAnzahl(),
                "auch zu viele Spalten werden gezaehlt - V01 soll das melden koennen");
    }

    @Test
    void ueberspringtLeerzeilen() throws IOException {
        List<Rohzeile> zeilen = lies(KOPF,
                "2025-01,A,Nord,1,1,1,1,1,1",
                "",
                "   ",
                "2025-02,B,Nord,1,1,1,1,1,1");

        assertEquals(2, zeilen.size(), "Leerzeilen erzeugen keine Geister-Rohzeilen");

        // Die Zeilennummern bleiben trotzdem die aus der Datei - sonst zeigt
        // eine Fehlermeldung nach einer Leerzeile auf die falsche Stelle.
        assertEquals(2, zeilen.get(0).zeilennummer());
        assertEquals(5, zeilen.get(1).zeilennummer());
    }

    @Test
    void veraendertDieWerteNicht() throws IOException {
        // Der Einleser bewahrt den Rohzustand: kein trim(), kein Parsen.
        // Leerzeichen muss die Validierung spaeter melden koennen.
        List<Rohzeile> zeilen = lies(KOPF,
                "2025-02, Produkt C ,Nord, 2000 ,2233.0,25.0,26.54,12.0,2000");

        assertEquals(" Produkt C ", zeilen.get(0).produkt());
        assertEquals(" 2000 ", zeilen.get(0).planMenge());
    }

    @Test
    void nurKopfzeileErgibtLeereListe() throws IOException {
        assertTrue(lies(KOPF).isEmpty());
    }

    @Test
    void leereDateiErgibtLeereListe() throws IOException {
        assertTrue(lies("").isEmpty());
    }
}
