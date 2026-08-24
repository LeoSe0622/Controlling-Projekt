package de.leo.controlling.pruefung.regeln;

import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Schweregrad;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests fuer V09 - Ist-Werte fuer Monate, die noch nicht vorbei sind.
 *
 * <p>Der Berichtsmonat wird fest vorgegeben statt aus der Uhr geholt. Ein Test, der
 * {@code YearMonth.now()} braucht, wird irgendwann von selbst rot oder gruen - und dann
 * weiss niemand mehr, ob der Code kaputt ist oder nur der Kalender weitergelaufen.
 */
class ZukunftsmonatRegelTest {

    private static final YearMonth BERICHTSMONAT = YearMonth.of(2026, 8);

    private final ZukunftsmonatRegel regel = new ZukunftsmonatRegel(BERICHTSMONAT);

    @Test
    void meldetNichts_beiVergangenemMonat() {
        assertTrue(regel.pruefe(zeileMitMonat("2025-03")).isEmpty());
    }

    /**
     * Der Berichtsmonat selbst ist noch nicht vorbei und trotzdem in Ordnung. Wuerde die
     * Regel hier anschlagen, meldete sie bei JEDEM Monatsbericht den aktuellen Monat -
     * und der Leser gewoehnt sich an, sie zu ueberlesen.
     */
    @Test
    void meldetNichts_imBerichtsmonatSelbst() {
        assertTrue(regel.pruefe(zeileMitMonat("2026-08")).isEmpty());
    }

    @Test
    void meldetWarnung_beiKommendemMonat() {
        List<Befund> befunde = regel.pruefe(zeileMitMonat("2026-09"));

        assertEquals(1, befunde.size());

        Befund b = befunde.get(0);
        assertEquals("V09", b.regelId());
        assertEquals("monat", b.feld());
        assertEquals("2026-09", b.originalwert());
        assertEquals(Schweregrad.WARNUNG, b.grad(),
                "die Zahlen sind rechenbar - die Zeile fliegt nicht raus, sie wird markiert");
    }

    @Test
    void meldetWarnung_beiSpaeteremJahr() {
        assertEquals(1, regel.pruefe(zeileMitMonat("2027-01")).size());
    }

    /**
     * Schweigegrundsatz: Fuer leere und unlesbare Monate sind V02 und V04 zustaendig.
     * Wuerde V09 hier mitreden, erzeugte eine einzige kaputte Zelle drei Befunde.
     */
    @Test
    void schweigt_beiUnlesbaremOderLeeremMonat() {
        assertTrue(regel.pruefe(zeileMitMonat("")).isEmpty(), "leer");
        assertTrue(regel.pruefe(zeileMitMonat("Maerz")).isEmpty(), "kein Datum");
        assertTrue(regel.pruefe(zeileMitMonat("2026-13")).isEmpty(), "Monat 13");
    }

    private static Rohzeile zeileMitMonat(String monat) {
        return new Rohzeile(7, 9, monat, "Produkt A", "Vertrieb Nord",
                "1000", "957.0", "50.0", "53.83", "30.0", "5000");
    }
}
