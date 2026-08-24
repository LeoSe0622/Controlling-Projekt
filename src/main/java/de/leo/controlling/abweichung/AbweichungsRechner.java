package de.leo.controlling.abweichung;

import de.leo.controlling.model.Datenzeile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Zerlegt die DB-Abweichung in Preis-, Mengen-, Misch- und Fixkosteneffekt.
 *
 * <p><b>Warum je ZEILE zerlegt wird und nicht je Jahr:</b> Auf Jahresebene gibt es keinen
 * Preis. Ein Produkt hat je Monat einen; einen Jahres-Planpreis muesste man als Durchschnitt
 * bilden, und damit entstuende ein zusaetzlicher Struktureffekt (welcher Monat wie schwer
 * wiegt), der die Zerlegung verfaelscht.
 *
 * <p>Deshalb: zerlegen, wo die Preise definiert sind — je Produkt und Monat — und dann die
 * EFFEKTE summieren. Weil die Zerlegung je Zeile exakt aufgeht, geht auch ihre Summe exakt auf.
 */
public final class AbweichungsRechner {

    private static final int SKALA_GELD = 2;

    /**
     * Zerlegt die Abweichung einer einzelnen Produkt-Monats-Zeile.
     *
     * <p>Rechenbeispiel (m_plan 2000, p_plan 25,00, m_ist 2233, p_ist 26,54, k 12,00):
     * <pre>
     *   Preisabweichung  =  1,54 x 2.000 = 3.080,00
     *   Mengenabweichung =   233 x 13,00 = 3.029,00
     *   Mischabweichung  =  1,54 x   233 =   358,82
     *   Summe                            = 6.467,82  = DB II ist - plan
     * </pre>
     */
    public Abweichung jeZeile(Datenzeile z) {
    
        BigDecimal preisDiff = z.istPreis().subtract(z.planPreis());
        BigDecimal mengenDiff = z.istMenge().subtract(z.planMenge());
        
        BigDecimal preis = preisDiff.multiply(z.planMenge()).setScale(SKALA_GELD, RoundingMode.HALF_UP);
        BigDecimal mengen = mengenDiff.multiply(z.planPreis().subtract(z.variableStueckkosten())).setScale(SKALA_GELD, RoundingMode.HALF_UP);
        BigDecimal misch = preisDiff.multiply(mengenDiff).setScale(SKALA_GELD, RoundingMode.HALF_UP);
        BigDecimal fix = BigDecimal.ZERO.setScale(SKALA_GELD);
        return new Abweichung(preis, mengen, misch, fix);
    }

    /**
     * Zerlegt die Abweichung je Produkt: gruppiert die Zeilen nach Produkt,
     * zerlegt jede einzeln und summiert die EFFEKTE.
     *
     * <p>Rueckgabe ist eine Map statt einer Liste, damit App die Abweichung per
     * Produktnamen zum passenden Produktergebnis legen kann - ohne sich darauf zu
     * verlassen, dass zwei getrennt erzeugte Listen dieselbe Reihenfolge haben.
     * Solche stillen Kopplungen brechen genau dann, wenn man sie am wenigsten
     * erwartet.
     *
     * @return TreeMap: alphabetisch nach Produkt, damit der Bericht stabil bleibt
     */
    public Map<String, Abweichung> jeProdukt(List<Datenzeile> zeilen) {

        Map<String, List<Datenzeile>> nachProdukt = new TreeMap<>();
        for (Datenzeile z : zeilen) {
            nachProdukt.computeIfAbsent(z.produkt(), k -> new ArrayList<>()).add(z);
        }
        
        Map<String, Abweichung> ergebnis = new TreeMap<>();
        for (Map.Entry<String, List<Datenzeile>> e : nachProdukt.entrySet()) {
            ergebnis.put(e.getKey(), summe(
                    e.getValue().stream().map(this::jeZeile).toList()));
        }
        return ergebnis;
    }

    /**
     * Summiert die Abweichungen mehrerer Zeilen - etwa alle Monate eines Produkts.
     * Weil jede Einzelzerlegung exakt aufgeht, gilt das auch fuer die Summe.
     */
    public Abweichung summe(List<Abweichung> teile) {

        BigDecimal preis = BigDecimal.ZERO.setScale(SKALA_GELD);
        BigDecimal mengen = BigDecimal.ZERO.setScale(SKALA_GELD);
        BigDecimal misch = BigDecimal.ZERO.setScale(SKALA_GELD);
        BigDecimal fix = BigDecimal.ZERO.setScale(SKALA_GELD);
        for (Abweichung t : teile) {
            preis = preis.add(t.preisabweichung());
            mengen = mengen.add(t.mengenabweichung());
            misch = misch.add(t.mischabweichung());
            fix = fix.add(t.fixkostenabweichung());
        }
        return new Abweichung(preis, mengen, misch, fix);
    }
}
