package de.leo.controlling.pruefung.regeln;

import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Schweregrad;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StueckdbRegelTest {

    private final StueckdbRegel regel = new StueckdbRegel();

    @Test
    void meldetNichts_beiGesunderMarge() {
        // Produkt C: Kosten 12, Preis 25 - gesunde Marge
        List<Befund> befunde = regel.pruefe(gueltigeZeile());

        assertTrue(befunde.isEmpty(), "12 < 25 - der Stueck-DB ist positiv");
    }

    @Test
    void meldetWarnung_wennKostenUeberPreis() {
        // Kosten 30 bei einem Preis von 25 - jedes Stueck macht Verlust.
        Rohzeile zeile = new Rohzeile(5, 9, "2025-02", "Produkt C", "Vertrieb Nord",
                "2000", "2233.0", "25.0", "25.0", "30.0", "2000");

        List<Befund> befunde = regel.pruefe(zeile);

        // Zwei Befunde: planPreis und istPreis liegen beide unter den Kosten.
        // Die Reihenfolge entspricht der Aufrufreihenfolge in pruefe().
        assertEquals(2, befunde.size());

        assertEquals("planPreis", befunde.get(0).feld());
        assertEquals("istPreis", befunde.get(1).feld());

        for (Befund b : befunde) {
            assertEquals("V06", b.regelId());
            assertEquals(Schweregrad.WARNUNG, b.grad(),
                    "V06 ist eine WARNUNG - mit dem Wert kann man rechnen, er ist nur erklaerungsbeduerftig");
            assertEquals(5, b.zeilennummer());
        }
    }

    @Test
    void meldetNichts_beiNegativemPreis() {
        // Das ist CSV-Zeile 14. Rechnerisch waere 45 >= -77.05 wahr - aber der
        // negative Preis ist V05s Thema. Ohne diesen Schweigefall bekaeme Zeile 14
        // zwei Befunde fuer ein Problem.
        Rohzeile zeile = new Rohzeile(14, 9, "2025-09", "Produkt B", "Vertrieb Sued",
                "600", "761.0", "80.0", "-77.05", "45.0", "3000");

        List<Befund> befunde = regel.pruefe(zeile);

        assertTrue(befunde.isEmpty(), "negativer Preis gehoert V05, nicht V06");
    }

    /** Produkt C, CSV-Zeile 2: Kosten 12, Preis 25. */
    private static Rohzeile gueltigeZeile() {
        return new Rohzeile(2, 9, "2025-02", "Produkt C", "Vertrieb Nord",
                "2000", "2233.0", "25.0", "26.54", "12.0", "2000");
    }
}
