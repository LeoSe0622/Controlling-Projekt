package de.leo.controlling.report;

import de.leo.controlling.abweichung.Ampel;
import de.leo.controlling.io.CsvEinleser;
import de.leo.controlling.rechnung.Produktergebnis;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueft die abgeleiteten Methoden des Berichtsmodells gegen die echten Daten.
 *
 * <p>Das Modell rechnet selbst nichts - es fasst zusammen, was die Module geliefert
 * haben. Genau deshalb sind diese Tests kurz und trotzdem wertvoll: Sie sichern die
 * fuenf Aussagen ab, auf die sich SPAETER beide Ausgabeformate verlassen.
 */
class BerichtsmodellTest {

    /** Fester Zeitstempel - das Modell bekommt die Zeit uebergeben, statt sie zu holen. */
    private static final LocalDateTime STICHTAG = LocalDateTime.of(2025, 12, 31, 23, 59);

    private static Berichtsmodell modell() throws IOException {
        return new BerichtsmodellBauer().baue(
                "controlling_rohdaten.csv",
                new CsvEinleser().lies(Path.of("controlling_rohdaten.csv")),
                STICHTAG);
    }

    @Test
    void modellEnthaeltAlleProdukteUndZeilen() throws IOException {
        Berichtsmodell m = modell();

        assertEquals(4, m.produkte().size());
        assertEquals(49, m.alleZeilen().size());
        assertEquals(43, m.protokoll().verwertbareZeilen().size());
        assertEquals(12, m.erwarteteMonate());
        assertEquals(STICHTAG, m.erstelltAm());
    }

    @Test
    void findetALLEunvollstaendigenProdukte() {
        // DER Test, der den Schleifen-return gefunden haette: A (11/12), B (9/12)
        // und C (11/12) sind unvollstaendig, nur D hat alle zwoelf Monate.
        // Eine Methode, die beim ersten Treffer aufhoert, liefert hier 1 statt 3 -
        // und Produkt B bliebe im Bericht ohne Hinweis.
        List<Produktergebnis> unvollstaendig;
        try {
            unvollstaendig = modell().unvollstaendigeProdukte();
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        assertEquals(3, unvollstaendig.size());
        assertEquals(List.of("Produkt A", "Produkt B", "Produkt C"),
                unvollstaendig.stream().map(Produktergebnis::produkt).toList());
    }

    @Test
    void abstimmbrueckeGehtAuf() throws IOException {
        Berichtsmodell m = modell();

        assertEquals(0, m.brueckenDifferenz().signum(),
                "Differenz: " + m.brueckenDifferenz());
        assertTrue(m.brueckeGehtAuf());
    }

    @Test
    void gesamtabweichungStimmtMitDenDeckungsbeitraegenUeberein() throws IOException {
        Berichtsmodell m = modell();

        BigDecimal ausDeckungsbeitrag = m.gesamtIst().dbZwei()
                .subtract(m.gesamtPlan().dbZwei());

        assertEquals(0, m.gesamtAbweichung().gesamt().compareTo(ausDeckungsbeitrag));
        assertEquals(new BigDecimal("730331.65"), m.gesamtAbweichung().gesamt());
    }

    @Test
    void ampelnStimmen() throws IOException {
        Berichtsmodell m = modell();

        // A: -716,17 = -0,43 % -> nur Euro-Schwelle gerissen        -> GELB
        // B: -16.175,64 = -9,99 % -> beide gerissen und negativ      -> ROT
        // C: +21.070,54 = +7,98 % -> beide gerissen, aber positiv    -> GELB
        // D: +726.152,92 -> beide gerissen, aber positiv             -> GELB
        assertEquals(Ampel.GELB, m.ampelFuer(produkt(m, "Produkt A")));
        assertEquals(Ampel.ROT, m.ampelFuer(produkt(m, "Produkt B")));
        assertEquals(Ampel.GELB, m.ampelFuer(produkt(m, "Produkt C")));
        assertEquals(Ampel.GELB, m.ampelFuer(produkt(m, "Produkt D")));
    }

    private static Produktergebnis produkt(Berichtsmodell m, String name) {
        return m.produkte().stream()
                .filter(p -> p.produkt().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Produkt fehlt: " + name));
    }
}
