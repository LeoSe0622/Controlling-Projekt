package de.leo.controlling.rechnung;

import de.leo.controlling.model.Datenzeile;
import de.leo.controlling.model.Rohzeile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GesamtergebnisTest {

    private final ProduktRechner rechner = new ProduktRechner();

    /** Produkt C, Februar: Plan-DB II = 24.000,00 */
    private static Datenzeile cFebruar() {
        return Datenzeile.aus(new Rohzeile(2, 9, "2025-02", "Produkt C", "Vertrieb Nord",
                "2000", "2233.0", "25.0", "26.54", "12.0", "2000"));
    }

    /** Produkt D, Januar: Plan 350 x 120 - 350 x 70 - 4.000 = 13.500,00 */
    private static Datenzeile dJanuar() {
        return Datenzeile.aus(new Rohzeile(39, 9, "2025-01", "Produkt D", "Vertrieb Nord",
                "350", "394.0", "120.0", "123.68", "70.0", "4000"));
    }

    @Test
    void betriebsergebnisIstSummeAllerProdukte() {
        List<Produktergebnis> ergebnisse = rechner.jeProdukt(List.of(cFebruar(), dJanuar()));

        // Handrechnung Plan:
        //   Produkt C: 2000 x 25,00 - 2000 x 12,00 - 2.000 = 24.000,00
        //   Produkt D:  350 x 120,00 - 350 x 70,00 - 4.000 = 13.500,00
        //   Betriebsergebnis                                 = 37.500,00
        assertEquals(new BigDecimal("37500.00"), rechner.gesamtPlan(ergebnisse).dbZwei());

        // Umsatz Plan: 50.000,00 + 42.000,00 = 92.000,00
        assertEquals(new BigDecimal("92000.00"), rechner.gesamtPlan(ergebnisse).umsatz());
    }

    @Test
    void betriebsergebnisIstSeite() {
        List<Produktergebnis> ergebnisse = rechner.jeProdukt(List.of(cFebruar(), dJanuar()));

        // Handrechnung Ist:
        //   Produkt C: 2233,0 x 26,54 = 59.263,82 ; var 26.796,00 ; DB II = 30.467,82
        //   Produkt D:  394,0 x 123,68 = 48.729,92 ; var 27.580,00 ; DB II = 17.149,92
        //   Betriebsergebnis                                          = 47.617,74
        assertEquals(new BigDecimal("47617.74"), rechner.gesamtIst(ergebnisse).dbZwei());
    }

    @Test
    void leereListeErgibtNullen() {
        assertEquals(new BigDecimal("0.00"), rechner.gesamtPlan(List.of()).dbZwei());
    }
}
