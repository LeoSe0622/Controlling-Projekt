package de.leo.controlling.rechnung;

import de.leo.controlling.model.Datenzeile;
import de.leo.controlling.model.Rohzeile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProduktRechnerTest {

    private final ProduktRechner rechner = new ProduktRechner();

    /** Produkt C, Januar: Plan 2000 x 25,00 - Ist 1984 x 24,07 - k 12,00 - fix 2000 */
    private static Datenzeile cJanuar() {
        return Datenzeile.aus(new Rohzeile(34, 9, "2025-01", "Produkt C", "Vertrieb Nord",
                "2000", "1984.0", "25.0", "24.07", "12.0", "2000"));
    }

    /** Produkt C, Februar: Plan 2000 x 25,00 - Ist 2233 x 26,54 - k 12,00 - fix 2000 */
    private static Datenzeile cFebruar() {
        return Datenzeile.aus(new Rohzeile(2, 9, "2025-02", "Produkt C", "Vertrieb Nord",
                "2000", "2233.0", "25.0", "26.54", "12.0", "2000"));
    }

    /** Produkt D, Januar: Plan 350 x 120,00 - Ist 394 x 123,68 - k 70,00 - fix 4000 */
    private static Datenzeile dJanuar() {
        return Datenzeile.aus(new Rohzeile(39, 9, "2025-01", "Produkt D", "Vertrieb Nord",
                "350", "394.0", "120.0", "123.68", "70.0", "4000"));
    }

    @Test
    void summiertZweiMonateEinesProdukts() {
        List<Produktergebnis> ergebnisse = rechner.jeProdukt(List.of(cJanuar(), cFebruar()));

        assertEquals(1, ergebnisse.size(), "beide Zeilen gehoeren zu einem Produkt");

        Produktergebnis c = ergebnisse.get(0);
        assertEquals("Produkt C", c.produkt());

        // Handrechnung Plan: 2x (2000 x 25,00) = 100.000,00 Umsatz
        //                    2x (2000 x 12,00) =  48.000,00 variable Kosten
        //                    DB I = 52.000,00 ; Fixkosten 2x 2000 = 4.000,00 ; DB II = 48.000,00
        assertEquals(new BigDecimal("100000.00"), c.plan().umsatz());
        assertEquals(new BigDecimal("48000.00"), c.plan().variableKosten());
        assertEquals(new BigDecimal("52000.00"), c.plan().dbEins());
        assertEquals(new BigDecimal("48000.00"), c.plan().dbZwei());

        // Handrechnung Ist: Jan 47.754,88 + Feb 59.263,82 = 107.018,70 Umsatz
        //                   Jan 23.808,00 + Feb 26.796,00 =  50.604,00 variable Kosten
        //                   DB I = 56.414,70 ; Fixkosten 4.000,00 ; DB II = 52.414,70
        assertEquals(new BigDecimal("107018.70"), c.ist().umsatz());
        assertEquals(new BigDecimal("50604.00"), c.ist().variableKosten());
        assertEquals(new BigDecimal("56414.70"), c.ist().dbEins());
        assertEquals(new BigDecimal("52414.70"), c.ist().dbZwei());
    }

    @Test
    void fixkostenWerdenJeMonatGezaehlt() {
        // Die Falle aus dem Plan: fixkosten_produkt steht auf JEDER Zeile.
        // Zwei Monate ergeben 2 x 2000 = 4000 - richtig, weil es ein Monatswert ist.
        // Falsch waere es nur, wenn eine Dublette mitgezaehlt wuerde; genau davor
        // schuetzt Regel V07.
        Produktergebnis c = rechner.jeProdukt(List.of(cJanuar(), cFebruar())).get(0);

        assertEquals(new BigDecimal("4000.00"), c.plan().fixkosten());
        assertEquals(new BigDecimal("4000.00"), c.ist().fixkosten());
    }

    @Test
    void zaehltVerschiedeneMonate() {
        Produktergebnis c = rechner.jeProdukt(List.of(cJanuar(), cFebruar())).get(0);

        assertEquals(2, c.monate());
    }

    @Test
    void trenntProdukteUndSortiertAlphabetisch() {
        // Absichtlich in falscher Reihenfolge hineingegeben.
        List<Produktergebnis> ergebnisse =
                rechner.jeProdukt(List.of(dJanuar(), cFebruar(), cJanuar()));

        assertEquals(2, ergebnisse.size());
        assertEquals("Produkt C", ergebnisse.get(0).produkt());
        assertEquals("Produkt D", ergebnisse.get(1).produkt());

        assertEquals(2, ergebnisse.get(0).monate());
        assertEquals(1, ergebnisse.get(1).monate());
    }

    @Test
    void abweichungHatDasRichtigeVorzeichen() {
        // Produkt C hat im Februar mehr verkauft UND teurer - Ist muss ueber Plan liegen,
        // die Abweichung also positiv sein. "+" heisst ergebnisverbessernd.
        Produktergebnis c = rechner.jeProdukt(List.of(cFebruar())).get(0);

        // DB II Ist 30.467,82 - Plan 24.000,00 = +6.467,82
        assertEquals(new BigDecimal("6467.82"), c.dbZweiAbweichung());
    }

    @Test
    void leereEingabeErgibtLeeresErgebnis() {
        assertEquals(List.of(), rechner.jeProdukt(List.of()));
    }
}
