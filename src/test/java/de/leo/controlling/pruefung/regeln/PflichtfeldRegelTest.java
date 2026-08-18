package de.leo.controlling.pruefung.regeln;

import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Schweregrad;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test zu {@link PflichtfeldRegel} (V02).
 *
 * <p>Aufbau jedes Tests nach dem Muster <b>Arrange - Act - Assert</b>:
 * Situation herstellen, Methode aufrufen, Ergebnis pruefen.
 */
class PflichtfeldRegelTest {

    private final PflichtfeldRegel regel = new PflichtfeldRegel();

    @Test
    void meldetNichts_wennAlleFelderGefuellt() {
        Rohzeile zeile = gueltigeZeile();          // Arrange

        List<Befund> befunde = regel.pruefe(zeile); // Act
        assertTrue(befunde.isEmpty());       
        assertTrue(befunde.isEmpty(), "gueltige Zeile darf keinen Befund erzeugen");
    }

    @Test
    void meldetFehler_wennIstMengeLeer() {
        // Das ist CSV-Zeile 22 aus deinen echten Daten: istMenge ist leer.
        Rohzeile zeile = new Rohzeile(22, 9, "2025-04", "Produkt B", "Vertrieb Sued",
                "600", "", "80.0", "83.03", "45.0", "3000");

        List<Befund> befunde = regel.pruefe(zeile);

        
        assertEquals(1, befunde.size());
        
        Befund b = befunde.get(0);

        assertEquals("istMenge", b.feld());
        assertEquals(Schweregrad.Grad.Fehler, b.grad());
        assertEquals(22, b.Zeilennummer());
        assertEquals("", b.originalwert());
    }

    /** Eine vollstaendig gefuellte Zeile — das ist CSV-Zeile 2. */
    private static Rohzeile gueltigeZeile() {
        return new Rohzeile(2, 9, "2025-02", "Produkt C", "Vertrieb Nord",
                "2000", "2233.0", "25.0", "26.54", "12.0", "2000");
    }
}
