package de.leo.controlling.pruefung.regeln;

import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Schweregrad;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NegativwertRegelTest {

    private final NegativwertRegel regel = new NegativwertRegel();

    @Test
    void meldetNichts_beiGueltigerZeile() {
        List<Befund> befunde = regel.pruefe(gueltigeZeile());

        assertTrue(befunde.isEmpty(), "alle Werte positiv - kein Befund erwartet");
    }

    @Test
    void meldetFehler_beiNegativemIstPreis() {
        // Das ist CSV-Zeile 14 aus deinen echten Daten.
        Rohzeile zeile = new Rohzeile(14, 9, "2025-09", "Produkt B", "Vertrieb Sued",
                "600", "761.0", "80.0", "-77.05", "45.0", "3000");

        List<Befund> befunde = regel.pruefe(zeile);

        assertEquals(1, befunde.size());

        Befund b = befunde.get(0);
        assertEquals("istPreis", b.feld());
        assertEquals("V05", b.regeId());
        assertEquals(Schweregrad.Grad.Fehler, b.grad());
        assertEquals("-77.05", b.originalwert());
        assertEquals(14, b.Zeilennummer());
    }

    @Test
    void meldetNichts_beiLeeremFeld() {
        // Schweigegrundsatz, hier gleich doppelt: Zahlen.parse() liefert sowohl bei
        // leer als auch bei "keine Zahl" null - ein einziges if deckt V02 und V03 ab.
        Rohzeile zeile = new Rohzeile(22, 9, "2025-04", "Produkt B", "Vertrieb Sued",
                "600", "", "80.0", "83.03", "45.0", "3000");

        List<Befund> befunde = regel.pruefe(zeile);

        assertTrue(befunde.isEmpty(), "leeres Feld gehoert V02, nicht V05");
    }

    /** Eine vollstaendig gefuellte, fachlich saubere Zeile — CSV-Zeile 2. */
    private static Rohzeile gueltigeZeile() {
        return new Rohzeile(2, 9, "2025-02", "Produkt C", "Vertrieb Nord",
                "2000", "2233.0", "25.0", "26.54", "12.0", "2000");
    }
}
