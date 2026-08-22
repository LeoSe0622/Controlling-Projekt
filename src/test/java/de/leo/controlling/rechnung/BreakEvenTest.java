package de.leo.controlling.rechnung;

import de.leo.controlling.model.Datenzeile;
import de.leo.controlling.model.Rohzeile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BreakEvenTest {

    private final DeckungsbeitragsRechner rechner = new DeckungsbeitragsRechner();

    @Test
    void breakEvenWirdAufgerundet() {
        // Produkt C, Februar: Fixkosten 2.000, Stueck-DB Plan = 25,00 - 12,00 = 13,00
        // 2.000 / 13,00 = 153,84... -> 154 Stueck
        // Aufrunden, nicht kaufmaennisch: bei 153 Stueck sind die Fixkosten NICHT gedeckt.
        Datenzeile z = Datenzeile.aus(new Rohzeile(2, 9, "2025-02", "Produkt C", "Vertrieb Nord",
                "2000", "2233.0", "25.0", "26.54", "12.0", "2000"));

        assertEquals(new BigDecimal("154"), rechner.plan(z).breakEvenMenge());

        // Ist-Seite: Stueck-DB = 26,54 - 12,00 = 14,54 -> 2.000 / 14,54 = 137,5... -> 138
        assertEquals(new BigDecimal("138"), rechner.ist(z).breakEvenMenge());
    }

    @Test
    void keinBreakEvenBeiNegativemStueckDb() {
        // Kosten 30 bei Preis 25: Jedes verkaufte Stueck vergroessert den Verlust.
        // Es gibt keine Menge, ab der sich das traegt - also null, nicht eine
        // riesige oder negative Zahl.
        Datenzeile z = Datenzeile.aus(new Rohzeile(5, 9, "2025-02", "Produkt C", "Vertrieb Nord",
                "2000", "2000", "25.0", "25.0", "30.0", "2000"));

        assertNull(rechner.ist(z).breakEvenMenge(),
                "bei negativem Stueck-DB gibt es keinen Break-Even");
    }

    @Test
    void keinBreakEvenOhneMenge() {
        // Ohne Menge gibt es keinen Stueck-DB - und damit auch keinen Break-Even.
        Datenzeile z = Datenzeile.aus(new Rohzeile(9, 9, "2025-02", "Produkt C", "Vertrieb Nord",
                "2000", "0", "25.0", "26.54", "12.0", "2000"));

        assertNull(rechner.ist(z).breakEvenMenge());
    }
}
