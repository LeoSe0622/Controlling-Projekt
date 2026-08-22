package de.leo.controlling.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatenzeileTest {

    @Test
    void wandeltGueltigeZeileUm() {
        // CSV-Zeile 2
        Rohzeile roh = new Rohzeile(2, 9, "2025-02", "Produkt C", "Vertrieb Nord",
                "2000", "2233.0", "25.0", "26.54", "12.0", "2000");

        Datenzeile z = Datenzeile.aus(roh);

        assertEquals(2, z.zeilennummer());
        assertEquals(YearMonth.of(2025, 2), z.monat());
        assertEquals("Produkt C", z.produkt());
        assertEquals("Vertrieb Nord", z.kostenstelle());

        // BigDecimal.equals() beachtet die Nachkommastellen: Zahlen.parse("2233.0")
        // liefert scale 1, also muss der Sollwert ebenfalls "2233.0" heissen.
        // new BigDecimal("2233") waere NICHT gleich.
        assertEquals(new BigDecimal("2000"), z.planMenge());
        assertEquals(new BigDecimal("2233.0"), z.istMenge());
        assertEquals(new BigDecimal("25.0"), z.planPreis());
        assertEquals(new BigDecimal("26.54"), z.istPreis());
        assertEquals(new BigDecimal("12.0"), z.variableStueckkosten());
        assertEquals(new BigDecimal("2000"), z.fixkostenProdukt());
    }

    @Test
    void wirftBeiLeeremZahlenfeld() {
        // So eine Zeile duerfte hier nie ankommen - V02 haette sie aussortiert.
        // Kommt sie trotzdem an, ist das ein Programmierfehler und muss knallen.
        Rohzeile roh = new Rohzeile(22, 9, "2025-04", "Produkt B", "Vertrieb Sued",
                "600", "", "80.0", "83.03", "45.0", "3000");

        // Das Lambda verpackt den Aufruf, statt ihn auszufuehren - nur so kann
        // assertThrows ihn selbst ausloesen und die Exception fangen.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Datenzeile.aus(roh));

        // Die Meldung muss sagen, WO gesucht werden soll. Ohne Zeilennummer und
        // Feldname ist die Exception fast wertlos.
        assertTrue(ex.getMessage().contains("22"), "Meldung muss die Zeilennummer nennen");
        assertTrue(ex.getMessage().contains("istMenge"), "Meldung muss das Feld nennen");
    }

    @Test
    void wirftBeiUngueltigemMonat() {
        Rohzeile roh = new Rohzeile(5, 9, "2025-13", "Produkt C", "Vertrieb Nord",
                "2000", "2233.0", "25.0", "26.54", "12.0", "2000");

        assertThrows(IllegalArgumentException.class, () -> Datenzeile.aus(roh));
    }

    @Test
    void trimmtLeerzeichen() {
        // Hier interpretiert die Stufe die Rohdaten - deshalb ist Trimmen jetzt
        // richtig, obwohl es im CsvEinleser bewusst unterblieben ist.
        Rohzeile roh = new Rohzeile(5, 9, " 2025-02 ", " Produkt C ", " Vertrieb Nord ",
                " 2000 ", "2233.0", "25.0", "26.54", "12.0", "2000");

        Datenzeile z = Datenzeile.aus(roh);

        assertEquals(YearMonth.of(2025, 2), z.monat());
        assertEquals("Produkt C", z.produkt());
        assertEquals("Vertrieb Nord", z.kostenstelle());
        assertEquals(new BigDecimal("2000"), z.planMenge());
    }
}
