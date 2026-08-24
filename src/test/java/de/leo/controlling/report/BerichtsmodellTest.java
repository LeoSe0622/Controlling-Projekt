package de.leo.controlling.report;

import de.leo.controlling.Testdaten;
import de.leo.controlling.abweichung.Ampel;
import de.leo.controlling.rechnung.Produktergebnis;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueft die abgeleiteten Methoden des Berichtsmodells.
 *
 * <p>Das Modell rechnet selbst nichts — es fasst zusammen, was die Module geliefert haben.
 * Genau deshalb sind diese Tests kurz und trotzdem wertvoll: Sie sichern die Aussagen ab,
 * auf die sich spaeter beide Ausgabeformate verlassen.
 */
class BerichtsmodellTest {

    @Test
    void modellEnthaeltAlleProdukteUndZeilen() throws IOException {
        Berichtsmodell m = Testdaten.modell();

        assertEquals(4, m.produkte().size());
        assertEquals(18, m.alleZeilen().size());
        assertEquals(14, m.protokoll().verwertbareZeilen().size());
        assertEquals(4, m.erwarteteMonate());
        assertEquals(Testdaten.STICHTAG, m.erstelltAm());
    }

    /**
     * Der Test, der einen {@code return} innerhalb der Schleife gefunden haette: Produkt A
     * und B fehlt je ein Monat, C und D sind vollstaendig. Eine Methode, die beim ersten
     * Treffer aufhoert, liefert hier 1 statt 2 — und im Bericht bliebe eines der beiden
     * Produkte ohne Hinweis.
     */
    @Test
    void findetALLEunvollstaendigenProdukte() throws IOException {
        List<Produktergebnis> unvollstaendig = Testdaten.modell().unvollstaendigeProdukte();

        assertEquals(2, unvollstaendig.size());
        assertEquals(List.of("Produkt A", "Produkt B"),
                unvollstaendig.stream().map(Produktergebnis::produkt).toList());
    }

    @Test
    void abstimmbrueckeGehtAuf() throws IOException {
        Berichtsmodell m = Testdaten.modell();

        assertEquals(0, m.brueckenDifferenz().signum(),
                "Differenz: " + m.brueckenDifferenz());
        assertTrue(m.brueckeGehtAuf());
    }

    @Test
    void gesamtabweichungStimmtMitDenDeckungsbeitraegenUeberein() throws IOException {
        Berichtsmodell m = Testdaten.modell();

        BigDecimal ausDeckungsbeitrag = m.gesamtIst().dbZwei()
                .subtract(m.gesamtPlan().dbZwei());

        assertEquals(0, m.gesamtAbweichung().gesamt().compareTo(ausDeckungsbeitrag));
        assertEquals(new BigDecimal("249000.00"), m.gesamtPlan().dbZwei());
        assertEquals(new BigDecimal("719301.89"), m.gesamtAbweichung().gesamt());
    }

    /**
     * Die vier Ampelstufen decken alle drei Faelle der Bewertungsregel ab:
     * nur Euro-Schwelle gerissen, beide gerissen und negativ, beide gerissen und positiv.
     */
    @Test
    void ampelnStimmen() throws IOException {
        Berichtsmodell m = Testdaten.modell();

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

    /**
     * Der Ausreisser der Testdatei traegt die gesamte Abweichung - und mehr als das.
     *
     * <p>Genau der Fall, der den Bericht auf den echten Daten irrefuehrend machte: Neun
     * von 1756 Zeilen mit einer Ist-Menge um das Vielfache des Plans machten 102 % der
     * Gesamtabweichung aus. Hier ist es eine von vierzehn - dasselbe Muster im Kleinen.
     */
    @Test
    void beanstandeteZeilenTragenDieGesamteAbweichung() throws IOException {
        Berichtsmodell m = Testdaten.modell();

        assertEquals(1, m.warnzeilen().zeilen());
        assertEquals(new BigDecimal("13500.00"), m.warnzeilen().plan().dbZwei());
        assertEquals(new BigDecimal("741643.00"), m.warnzeilen().ist().dbZwei());
        assertEquals(new BigDecimal("728143.00"), m.warnzeilen().abweichung());
    }

    /**
     * Ohne die eine beanstandete Zeile steht ein MINUS da, wo der Bericht ein Plus
     * meldet. Beide Zahlen sind richtig gerechnet - nur die Kernaussage nicht.
     */
    @Test
    void ohneDieBeanstandetenZeilenDrehtSichDasVorzeichen() throws IOException {
        Berichtsmodell m = Testdaten.modell();

        assertEquals(1, m.gesamtAbweichung().gesamt().signum(), "der Bericht meldet ein Plus");
        assertEquals(new BigDecimal("-8841.11"), m.abweichungOhneWarnzeilen());
        assertTrue(m.warnzeilenKippenDasErgebnis());
    }

    /**
     * Ueber 100 % ist kein Rechenfehler, sondern die Aussage: Die uebrigen Zeilen zeigen
     * in die andere Richtung, also uebersteigt der Beitrag der beanstandeten Zeilen die
     * Summe. Eine Deckelung auf 100 % wuerde genau diese Information verstecken.
     */
    @Test
    void anteilDerWarnzeilenDarfUeberHundertProzentLiegen() throws IOException {
        assertEquals(new BigDecimal("1.0123"), Testdaten.modell().anteilDerWarnzeilen());
    }
}
