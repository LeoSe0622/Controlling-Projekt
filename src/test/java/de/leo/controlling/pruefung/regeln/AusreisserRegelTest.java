package de.leo.controlling.pruefung.regeln;

import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Schweregrad;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AusreisserRegelTest {

    private final AusreisserRegel regel = new AusreisserRegel();

    @Test
    void meldetNichts_beiNormalerAbweichung() {
        // CSV-Zeile 2: Menge 2233 zu Plan 2000 = 11,65 %, Preis 26.54 zu 25 = 6,16 %.
        List<Befund> befunde = regel.pruefe(gueltigeZeile());

        assertTrue(befunde.isEmpty(), "beide Abweichungen liegen deutlich unter 50 %");
    }

    @Test
    void meldetWarnung_beiAusreisser() {
        // Das ist CSV-Zeile 46: istMenge 17950 bei Plan 350 - Faktor 51.
        Rohzeile zeile = new Rohzeile(46, 9, "2025-06", "Produkt D", "Vertrieb Nord",
                "350", "17950.0", "120.0", "111.54", "70.0", "4000");

        List<Befund> befunde = regel.pruefe(zeile);

        // Nur EIN Befund: Der Preis (111.54 zu 120 = 7 %) liegt unter der Schwelle.
        assertEquals(1, befunde.size());

        Befund b = befunde.get(0);
        assertEquals("istMenge", b.feld());
        assertEquals("V08", b.regeId());
        assertEquals(Schweregrad.Grad.Warnung, b.grad(),
                "V08 ist eine WARNUNG - die Zeile bleibt in der Rechnung und verzerrt sichtbar");
        assertEquals("17950.0", b.originalwert());
        assertEquals(46, b.Zeilennummer());
    }

    @Test
    void meldetNichts_beiLeeremFeld() {
        // CSV-Zeile 22: istMenge ist leer -> V02s Thema.
        Rohzeile zeile = new Rohzeile(22, 9, "2025-04", "Produkt B", "Vertrieb Sued",
                "600", "", "80.0", "83.03", "45.0", "3000");

        List<Befund> befunde = regel.pruefe(zeile);

        assertTrue(befunde.isEmpty(), "leeres Feld gehoert V02, nicht V08");
    }

    @Test
    void meldetNichts_beiNegativemPreis() {
        // CSV-Zeile 14: istPreis -77.05 waere rechnerisch 196 % Abweichung -
        // gehoert aber V05. Ohne diesen Schweigefall waere Zeile 14 doppelt gemeldet.
        Rohzeile zeile = new Rohzeile(14, 9, "2025-09", "Produkt B", "Vertrieb Sued",
                "600", "761.0", "80.0", "-77.05", "45.0", "3000");

        List<Befund> befunde = regel.pruefe(zeile);

        assertTrue(befunde.isEmpty(), "negativer Wert gehoert V05, nicht V08");
    }

    @Test
    void meldetNichts_wennPlanNullIst() {
        // Plan-Menge 0: Es gibt keine sinnvolle prozentuale Abweichung von null,
        // und die Division wuerde knallen. Der wichtigste Test dieser Klasse -
        // er prueft nicht nur "kein Befund", sondern auch "keine ArithmeticException".
        Rohzeile zeile = new Rohzeile(9, 9, "2025-02", "Produkt C", "Vertrieb Nord",
                "0", "500.0", "25.0", "26.54", "12.0", "2000");

        List<Befund> befunde = regel.pruefe(zeile);

        assertTrue(befunde.isEmpty(), "Division durch null muss abgefangen sein");
    }

    /** CSV-Zeile 2: Menge 2233 zu Plan 2000, Preis 26.54 zu Plan 25. */
    private static Rohzeile gueltigeZeile() {
        return new Rohzeile(2, 9, "2025-02", "Produkt C", "Vertrieb Nord",
                "2000", "2233.0", "25.0", "26.54", "12.0", "2000");
    }
}
