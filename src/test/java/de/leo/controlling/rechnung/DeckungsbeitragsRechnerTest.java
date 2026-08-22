package de.leo.controlling.rechnung;

import de.leo.controlling.model.Datenzeile;
import de.leo.controlling.model.Rohzeile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Prueft gegen eine HANDRECHNUNG, nicht gegen das, was der Code gerade liefert.
 *
 * <p>Das ist der Unterschied zwischen einem Test und einer Momentaufnahme: Die Sollwerte
 * unten wurden auf Papier ermittelt, bevor es die Implementierung gab. Wuerde man sie
 * aus der Programmausgabe uebernehmen, testete man nur, dass sich nichts aendert -
 * nicht, dass es stimmt.
 */
class DeckungsbeitragsRechnerTest {

    private final DeckungsbeitragsRechner rechner = new DeckungsbeitragsRechner();

    /** CSV-Zeile 2: Produkt C, 2025-02. Plan 2000 x 25,00 - Ist 2233 x 26,54 - k 12,00 - fix 2000 */
    private static Datenzeile produktCFebruar() {
        return Datenzeile.aus(new Rohzeile(2, 9, "2025-02", "Produkt C", "Vertrieb Nord",
                "2000", "2233.0", "25.0", "26.54", "12.0", "2000"));
    }

    @Test
    void planSeiteStimmt() {
        Deckungsbeitrag db = rechner.plan(produktCFebruar());

        // Handrechnung:
        //   Umsatz          = 2000 x 25,00 = 50.000,00
        //   Variable Kosten = 2000 x 12,00 = 24.000,00
        //   DB I            = 50.000,00 - 24.000,00 = 26.000,00
        //   DB II           = 26.000,00 -  2.000,00 = 24.000,00

        // Sollwerte mit zwei Nachkommastellen: der Rechner rundet auf Skala 2,
        // und BigDecimal.equals() beachtet die Skala.
        assertEquals(new BigDecimal("50000.00"), db.umsatz());
        assertEquals(new BigDecimal("24000.00"), db.variableKosten());
        assertEquals(new BigDecimal("26000.00"), db.dbEins());
        assertEquals(new BigDecimal("24000.00"), db.dbZwei());
    }

    @Test
    void istSeiteStimmt() {
        Deckungsbeitrag db = rechner.ist(produktCFebruar());

        // Handrechnung:
        //   Umsatz          = 2233,0 x 26,54 = 59.263,82
        //   Variable Kosten = 2233,0 x 12,00 = 26.796,00
        //   DB I            = 59.263,82 - 26.796,00 = 32.467,82
        //   DB II           = 32.467,82 -  2.000,00 = 30.467,82

        assertEquals(new BigDecimal("59263.82"), db.umsatz());
        assertEquals(new BigDecimal("26796.00"), db.variableKosten());
        assertEquals(new BigDecimal("32467.82"), db.dbEins());
        assertEquals(new BigDecimal("30467.82"), db.dbZwei());
    }

    @Test
    void istSeiteIstBesserAlsPlan() {
        // Fachliche Gegenprobe: Produkt C hat im Februar mehr verkauft (2233 statt 2000)
        // UND einen hoeheren Preis erzielt (26,54 statt 25,00). Beides wirkt in dieselbe
        // Richtung, also MUSS der Ist-DB ueber dem Plan liegen. Wenn dieser Test rot
        // wird, stimmt ein Vorzeichen irgendwo.
        Deckungsbeitrag plan = rechner.plan(produktCFebruar());
        Deckungsbeitrag ist = rechner.ist(produktCFebruar());

        assertEquals(1, ist.dbEins().compareTo(plan.dbEins()),
                "Ist-DB I muss ueber dem Plan liegen");
        assertEquals(1, ist.dbZwei().compareTo(plan.dbZwei()),
                "Ist-DB II muss ueber dem Plan liegen");
    }

    @Test
    void kennzahlenStimmen() {
        Deckungsbeitrag plan = rechner.plan(produktCFebruar());
        Deckungsbeitrag ist = rechner.ist(produktCFebruar());

        // Handrechnung:
        //   DB I je Stueck Plan = 26.000,00 / 2000   = 13,00
        //   DB I je Stueck Ist  = 32.467,82 / 2233,0 = 14,54
        //   DB-I-Marge Plan     = 26.000,00 / 50.000,00 = 0,5200
        //   DB-I-Marge Ist      = 32.467,82 / 59.263,82 = 0,5478522984 -> 0,5479

        assertEquals(new BigDecimal("13.00"), plan.dbEinsJeStueck());
        assertEquals(new BigDecimal("14.54"), ist.dbEinsJeStueck());

        // Margen haben Skala 4 - das entspricht zwei Nachkommastellen in Prozent.
        assertEquals(new BigDecimal("0.5200"), plan.dbEinsMarge());
        assertEquals(new BigDecimal("0.5479"), ist.dbEinsMarge());

        //   DB-II-Marge Plan = 24.000,00 / 50.000,00 = 0,4800
        assertEquals(new BigDecimal("0.4800"), plan.dbZweiMarge());
    }

    @Test
    void keineKennzahlenOhneMenge() {
        // Ein Produkt, das im Ist gar nicht verkauft wurde.
        Datenzeile z = Datenzeile.aus(new Rohzeile(9, 9, "2025-02", "Produkt C", "Vertrieb Nord",
                "2000", "0", "25.0", "26.54", "12.0", "2000"));

        Deckungsbeitrag db = rechner.ist(z);

        // Ohne Menge gibt es keinen Stueck-DB und keine Marge. Nicht 0, sondern null:
        // 0 % waere eine Aussage ("nichts verdient"), hier gibt es aber gar keinen
        // Umsatz, auf den sich eine Marge beziehen koennte. Im Bericht steht "n/a".
        assertNull(db.dbEinsJeStueck(), "ohne Menge gibt es keinen Stueck-DB");
        assertNull(db.dbEinsMarge(), "ohne Umsatz gibt es keine Marge");
        assertNull(db.dbZweiMarge(), "ohne Umsatz gibt es keine Marge");

        // Der DB II ist trotzdem eine echte Zahl: Die Fixkosten fallen an,
        // egal ob verkauft wurde. Genau deshalb ist die Trennung DB I / DB II
        // fachlich interessant.
        assertEquals(new BigDecimal("0.00"), db.umsatz());
        assertEquals(new BigDecimal("0.00"), db.dbEins());
        assertEquals(new BigDecimal("-2000.00"), db.dbZwei());
    }
}
