package de.leo.controlling.rechnung;

import de.leo.controlling.io.CsvEinleser;
import de.leo.controlling.model.Datenzeile;
import de.leo.controlling.pruefung.Validator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueft die beiden Aggregationen fuer Zeitreihe und Kostenstellen
 * gegen die echten Daten.
 */
class AggregationenTest {

    private final ProduktRechner rechner = new ProduktRechner();

    private static List<Datenzeile> echteDaten() throws IOException {
        return new Validator()
                .pruefe(new CsvEinleser().lies(Path.of("controlling_rohdaten.csv")))
                .verwertbareZeilen().stream()
                .map(Datenzeile::aus)
                .toList();
    }

    @Test
    void zeitreiheHatEinenEintragJeProduktUndMonat() throws IOException {
        List<Monatsergebnis> zeitreihe = rechner.jeProduktUndMonat(echteDaten());

        // 43 verwertbare Zeilen, je (Produkt, Monat) genau eine -> 43 Eintraege.
        assertEquals(43, zeitreihe.size());

        // Sortiert: erst Produkt A alphabetisch, darin chronologisch ab Januar.
        assertEquals("Produkt A", zeitreihe.get(0).produkt());
        assertEquals(YearMonth.of(2025, 1), zeitreihe.get(0).monat());
    }

    @Test
    void zeitreiheZeigtDenAusreisserMonat() throws IOException {
        // Produkt D, Juni 2025 - das ist CSV-Zeile 46 mit istMenge 17950.
        Monatsergebnis juni = rechner.jeProduktUndMonat(echteDaten()).stream()
                .filter(e -> e.produkt().equals("Produkt D"))
                .filter(e -> e.monat().equals(YearMonth.of(2025, 6)))
                .findFirst()
                .orElseThrow();

        // Umsatz = 17950 x 111,54 = 2.002.143,00
        assertEquals(new BigDecimal("2002143.00"), juni.ist().umsatz());

        // Genau dafuer gibt es die Zeitreihe: Auf Jahresebene sieht man nur, DASS
        // Produkt D auffaellig ist. Hier sieht man, dass es an EINEM Monat liegt.
        Monatsergebnis mai = rechner.jeProduktUndMonat(echteDaten()).stream()
                .filter(e -> e.produkt().equals("Produkt D"))
                .filter(e -> e.monat().equals(YearMonth.of(2025, 5)))
                .findFirst()
                .orElseThrow();

        assertTrue(juni.ist().umsatz().compareTo(mai.ist().umsatz().multiply(new BigDecimal("40"))) > 0,
                "der Juni-Umsatz muss um Groessenordnungen ueber dem Mai liegen");
    }

    @Test
    void kostenstellenTeilenAlleZeilenAuf() throws IOException {
        List<Kostenstellenergebnis> stellen = rechner.jeKostenstelle(echteDaten());

        assertEquals(2, stellen.size());
        assertEquals("Vertrieb Nord", stellen.get(0).kostenstelle());
        assertEquals("Vertrieb Sued", stellen.get(1).kostenstelle());

        // Keine Zeile geht verloren, keine wird doppelt gezaehlt.
        assertEquals(43, stellen.get(0).zeilen() + stellen.get(1).zeilen());
    }

    @Test
    void kostenstellenSummeStimmtMitDemBetriebsergebnisUeberein() throws IOException {
        List<Datenzeile> daten = echteDaten();

        List<Kostenstellenergebnis> stellen = rechner.jeKostenstelle(daten);
        BigDecimal ueberKostenstellen = stellen.stream()
                .map(k -> k.ist().dbZwei())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ueberProdukte = rechner.gesamtIst(rechner.jeProdukt(daten)).dbZwei();

        // Zwei voellig verschiedene Gruppierungen derselben 43 Zeilen muessen
        // dieselbe Summe ergeben. Waere das nicht so, ginge unterwegs etwas
        // verloren oder wuerde doppelt gezaehlt.
        assertEquals(0, ueberKostenstellen.compareTo(ueberProdukte),
                "Kostenstellen: " + ueberKostenstellen + " vs. Produkte: " + ueberProdukte);
    }

    @Test
    void zeitreiheSummeStimmtMitDemProduktergebnisUeberein() throws IOException {
        List<Datenzeile> daten = echteDaten();

        BigDecimal ueberMonate = rechner.jeProduktUndMonat(daten).stream()
                .filter(e -> e.produkt().equals("Produkt C"))
                .map(e -> e.ist().dbZwei())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ausJahresergebnis = rechner.jeProdukt(daten).stream()
                .filter(p -> p.produkt().equals("Produkt C"))
                .findFirst().orElseThrow()
                .ist().dbZwei();

        assertEquals(0, ueberMonate.compareTo(ausJahresergebnis));
    }
}
