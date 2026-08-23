package de.leo.controlling.rechnung;

import de.leo.controlling.Testdaten;
import de.leo.controlling.model.Datenzeile;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prueft die beiden Aggregationen fuer Zeitreihe und Kostenstellen. */
class AggregationenTest {

    private final ProduktRechner rechner = new ProduktRechner();

    @Test
    void zeitreiheHatEinenEintragJeProduktUndMonat() throws IOException {
        List<Monatsergebnis> zeitreihe = rechner.jeProduktUndMonat(Testdaten.datenzeilen());

        assertEquals(14, zeitreihe.size());
        assertEquals("Produkt A", zeitreihe.get(0).produkt());
        assertEquals(YearMonth.of(2025, 1), zeitreihe.get(0).monat());
    }

    /**
     * Die Zeitreihe beantwortet, was das Jahresergebnis nicht kann: ob ein Produkt
     * durchgaengig auffaellig war oder nur in einem Monat.
     */
    @Test
    void zeitreiheZeigtDenAusreisserMonat() throws IOException {
        List<Monatsergebnis> zeitreihe = rechner.jeProduktUndMonat(Testdaten.datenzeilen());

        Monatsergebnis april = eintrag(zeitreihe, "Produkt D", YearMonth.of(2025, 4));
        Monatsergebnis maerz = eintrag(zeitreihe, "Produkt D", YearMonth.of(2025, 3));

        assertEquals(new BigDecimal("2002143.00"), april.ist().umsatz());
        assertTrue(april.ist().umsatz().compareTo(
                        maerz.ist().umsatz().multiply(new BigDecimal("40"))) > 0,
                "der Ausreissermonat muss um Groessenordnungen ueber den uebrigen liegen");
    }

    @Test
    void kostenstellenTeilenAlleZeilenAuf() throws IOException {
        List<Kostenstellenergebnis> stellen = rechner.jeKostenstelle(Testdaten.datenzeilen());

        assertEquals(2, stellen.size());
        assertEquals("Vertrieb Nord", stellen.get(0).kostenstelle());
        assertEquals("Vertrieb Sued", stellen.get(1).kostenstelle());

        assertEquals(14, stellen.get(0).zeilen() + stellen.get(1).zeilen(),
                "keine Zeile geht verloren, keine wird doppelt gezaehlt");
    }

    /**
     * Zwei voellig verschiedene Gruppierungen derselben Zeilen muessen dieselbe Summe
     * ergeben. Ginge unterwegs etwas verloren oder wuerde doppelt gezaehlt, koennte das
     * nicht aufgehen — unabhaengig davon, welche Daten vorliegen.
     */
    @Test
    void kostenstellenSummeStimmtMitDemBetriebsergebnisUeberein() throws IOException {
        List<Datenzeile> daten = Testdaten.datenzeilen();

        BigDecimal ueberKostenstellen = rechner.jeKostenstelle(daten).stream()
                .map(k -> k.ist().dbZwei())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ueberProdukte = rechner.gesamtIst(rechner.jeProdukt(daten)).dbZwei();

        assertEquals(0, ueberKostenstellen.compareTo(ueberProdukte),
                "Kostenstellen: " + ueberKostenstellen + " vs. Produkte: " + ueberProdukte);
    }

    /** Dieselbe Probe eine Ebene tiefer: Monate eines Produkts gegen sein Jahresergebnis. */
    @Test
    void zeitreiheSummeStimmtMitDemProduktergebnisUeberein() throws IOException {
        List<Datenzeile> daten = Testdaten.datenzeilen();

        for (Produktergebnis p : rechner.jeProdukt(daten)) {
            BigDecimal ueberMonate = rechner.jeProduktUndMonat(daten).stream()
                    .filter(e -> e.produkt().equals(p.produkt()))
                    .map(e -> e.ist().dbZwei())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertEquals(0, ueberMonate.compareTo(p.ist().dbZwei()), p.produkt());
        }
    }

    private static Monatsergebnis eintrag(List<Monatsergebnis> zeitreihe,
                                          String produkt, YearMonth monat) {
        return zeitreihe.stream()
                .filter(e -> e.produkt().equals(produkt) && e.monat().equals(monat))
                .findFirst()
                .orElseThrow(() -> new AssertionError("fehlt: " + produkt + " " + monat));
    }
}
