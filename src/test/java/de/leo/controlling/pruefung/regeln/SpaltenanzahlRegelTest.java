package de.leo.controlling.pruefung.regeln;

import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Schweregrad;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpaltenanzahlRegelTest {

    private final SpaltenanzahlRegel regel = new SpaltenanzahlRegel();

    @Test
    void meldetNichts_beiNeunSpalten() {
        Rohzeile zeile = new Rohzeile(2, 9, "2025-02", "Produkt C", "Vertrieb Nord",
                "2000", "2233.0", "25.0", "26.54", "12.0", "2000");

        List<Befund> befunde = regel.pruefe(zeile);

        assertTrue(befunde.isEmpty(), "9 Spalten sind korrekt - kein Befund erwartet");
    }

    @Test
    void meldetFehler_beiSiebenSpalten() {
        // Eine Zeile, bei der zwei Spalten fehlten - der Einleser hat sie mit "" aufgefuellt.
        Rohzeile zeile = new Rohzeile(7, 7, "2025-02", "Produkt C", "Vertrieb Nord",
                "2000", "2233.0", "25.0", "26.54", "", "");

        List<Befund> befunde = regel.pruefe(zeile);

        assertEquals(1, befunde.size());

        Befund b = befunde.get(0);
        assertEquals(7, b.zeilennummer());
        assertEquals("V01", b.regelId());
        assertEquals(Schweregrad.FEHLER, b.grad());
        assertEquals("7", b.originalwert());
    }
}
