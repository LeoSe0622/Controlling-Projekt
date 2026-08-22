package de.leo.controlling.abweichung;

import de.leo.controlling.model.Datenzeile;
import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.rechnung.DeckungsbeitragsRechner;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbweichungsRechnerTest {

    private final AbweichungsRechner rechner = new AbweichungsRechner();
    private final DeckungsbeitragsRechner dbRechner = new DeckungsbeitragsRechner();

    /** CSV-Zeile 2: Produkt C, 2025-02. Plan 2000 x 25,00 - Ist 2233 x 26,54 - k 12,00 - fix 2000 */
    private static Datenzeile cFebruar() {
        return Datenzeile.aus(new Rohzeile(2, 9, "2025-02", "Produkt C", "Vertrieb Nord",
                "2000", "2233.0", "25.0", "26.54", "12.0", "2000"));
    }

    @Test
    void zerlegtEineZeile() {
        Abweichung a = rechner.jeZeile(cFebruar());

        // Handrechnung:
        //   preisDiff  = 26,54 - 25,00 =   1,54
        //   mengenDiff = 2233  - 2000  = 233
        //   Preis  = 1,54 x 2.000            = 3.080,00
        //   Menge  =  233 x (25,00 - 12,00)  = 3.029,00
        //   Misch  = 1,54 x 233              =   358,82
        //   Fix    = -(2000 - 2000)          =     0,00
        assertEquals(new BigDecimal("3080.00"), a.preisabweichung());
        assertEquals(new BigDecimal("3029.00"), a.mengenabweichung());
        assertEquals(new BigDecimal("358.82"), a.mischabweichung());
        assertEquals(new BigDecimal("0.00"), a.fixkostenabweichung());
        assertEquals(new BigDecimal("6467.82"), a.gesamt());
    }

    @Test
    void abstimmbrueckeGehtAuf() {
        // DIE Kernprobe der ganzen Phase: Die Summe der Einzeleffekte muss exakt
        // der Differenz der Deckungsbeitraege entsprechen. Geht sie nicht auf,
        // ist die Zerlegung unvollstaendig - und dann sind alle Einzelwerte
        // wertlos, auch wenn jeder fuer sich plausibel aussieht.
        Datenzeile z = cFebruar();

        BigDecimal ausZerlegung = rechner.jeZeile(z).gesamt();
        BigDecimal ausDeckungsbeitrag = dbRechner.ist(z).dbZwei()
                .subtract(dbRechner.plan(z).dbZwei());

        assertEquals(0, ausZerlegung.compareTo(ausDeckungsbeitrag),
                "Abstimmbruecke: " + ausZerlegung + " != " + ausDeckungsbeitrag);
    }

    @Test
    void negativePreisabweichungBeiPreisrueckgang() {
        // Produkt A, Mai (CSV-Zeile 16): Ist-Preis 47,68 unter Plan 50,00,
        // Ist-Menge 969 unter Plan 1000. Beide Effekte muessen NEGATIV sein -
        // "+" heisst ergebnisverbessernd, hier ist beides schlechter.
        Datenzeile z = Datenzeile.aus(new Rohzeile(16, 9, "2025-05", "Produkt A", "Vertrieb Nord",
                "1000", "969.0", "50.0", "47.68", "30.0", "5000"));

        Abweichung a = rechner.jeZeile(z);

        //   preisDiff  = 47,68 - 50,00 = -2,32
        //   mengenDiff =   969 - 1000  = -31
        //   Preis = -2,32 x 1.000           = -2.320,00
        //   Menge =   -31 x (50,00 - 30,00) =   -620,00
        //   Misch = -2,32 x -31             =     71,92   <- positiv! zwei Minus
        assertEquals(new BigDecimal("-2320.00"), a.preisabweichung());
        assertEquals(new BigDecimal("-620.00"), a.mengenabweichung());
        assertEquals(new BigDecimal("71.92"), a.mischabweichung());
    }

    @Test
    void summiertMehrereZeilen() {
        Abweichung a = rechner.summe(List.of(
                rechner.jeZeile(cFebruar()),
                rechner.jeZeile(cFebruar())
        ));

        // Zweimal dieselbe Zeile -> alles verdoppelt.
        assertEquals(new BigDecimal("6160.00"), a.preisabweichung());
        assertEquals(new BigDecimal("12935.64"), a.gesamt());
    }

    @Test
    void leereListeErgibtNullen() {
        assertEquals(new BigDecimal("0.00"), rechner.summe(List.of()).gesamt());
    }
}
