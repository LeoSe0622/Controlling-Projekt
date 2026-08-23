package de.leo.controlling.pruefung.regeln;

import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Schweregrad;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonatsformatRegelTest {

    private final MonatsformatRegel regel = new MonatsformatRegel();

    @Test
    void meldetNichts_beiGueltigemMonat() {
        List<Befund> befunde = regel.pruefe(zeileMitMonat("2025-02"));

        assertTrue(befunde.isEmpty(), "2025-02 ist gueltig - kein Befund erwartet");
    }

    @Test
    void meldetFehler_beiMonatDreizehn() {
        List<Befund> befunde = regel.pruefe(zeileMitMonat("2025-13"));

        assertEquals(1, befunde.size());

        Befund b = befunde.get(0);
        assertEquals("monat", b.feld());
        assertEquals("V04", b.regelId());
        assertEquals(Schweregrad.FEHLER, b.grad());
        assertEquals("2025-13", b.originalwert());
    }

    @Test
    void meldetNichts_beiLeeremMonat() {
        // Schweigegrundsatz: Ein leeres Pflichtfeld meldet V02, nicht V04.
        // Wuerde V04 hier auch anschlagen, bekaeme Zeile 22 zwei Befunde fuer ein Problem.
        List<Befund> befunde = regel.pruefe(zeileMitMonat(""));

        assertTrue(befunde.isEmpty(), "leerer Monat ist V02s Thema, nicht V04s");
    }

    /** Baut eine gueltige Zeile mit frei waehlbarem Monat. */
    private static Rohzeile zeileMitMonat(String monat) {
        return new Rohzeile(2, 9, monat, "Produkt C", "Vertrieb Nord",
                "2000", "2233.0", "25.0", "26.54", "12.0", "2000");
    }
}
