package de.leo.controlling.pruefung.regeln;

import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Schweregrad;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZahlformatRegelTest {

    private final ZahlformatRegel regel = new ZahlformatRegel();

    @Test
    void meldetNichts_beiGueltigerZeile() {
        List<Befund> befunde = regel.pruefe(gueltigeZeile());

        assertTrue(befunde.isEmpty(), "alle Zahlenfelder sind gueltig - kein Befund erwartet");
    }

    @Test
    void meldetFehler_beiTextStattZahl() {
        // istMenge enthaelt Text statt einer Zahl
        Rohzeile zeile = new Rohzeile(5, 9, "2025-02", "Produkt C", "Vertrieb Nord",
                "2000", "zweitausend", "25.0", "26.54", "12.0", "2000");

        List<Befund> befunde = regel.pruefe(zeile);

        assertEquals(1, befunde.size());

        Befund b = befunde.get(0);
        assertEquals("istMenge", b.feld());
        assertEquals("V03", b.regelId());
        assertEquals(Schweregrad.FEHLER, b.grad());
        assertEquals("zweitausend", b.originalwert());
        assertEquals(5, b.zeilennummer());
    }

    @Test
    void meldetNichts_beiLeeremFeld() {
        // Schweigegrundsatz: Ein leeres Pflichtfeld meldet V02, nicht V03.
        // Ohne diesen Test bekaeme Zeile 22 zwei Befunde fuer ein Problem.
        Rohzeile zeile = new Rohzeile(22, 9, "2025-04", "Produkt B", "Vertrieb Sued",
                "600", "", "80.0", "83.03", "45.0", "3000");

        List<Befund> befunde = regel.pruefe(zeile);

        assertTrue(befunde.isEmpty(), "leeres Feld ist V02s Thema, nicht V03s");
    }

    /** Eine vollstaendig gefuellte Zeile — CSV-Zeile 2. */
    private static Rohzeile gueltigeZeile() {
        return new Rohzeile(2, 9, "2025-02", "Produkt C", "Vertrieb Nord",
                "2000", "2233.0", "25.0", "26.54", "12.0", "2000");
    }
}
