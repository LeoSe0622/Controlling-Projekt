package de.leo.controlling.abweichung;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Die Testfaelle sind die vier echten Produkte plus die Randbedingungen.
 * Schwellen: 500,00 EUR und 5 %.
 */
class WesentlichkeitTest {

    private final Wesentlichkeit w = Wesentlichkeit.standard();

    @Test
    void negativerPlanwertKipptDasProzentkriteriumNicht() {
        // Ein Produkt, das planmaessig Verlust macht: Plan-DB II = -10.000.
        // Ohne abs() auf BEIDEN Seiten waere der Quotient negativ und der
        // Vergleich mit 5 % immer falsch - die Prozent-Schwelle wuerde
        // stillschweigend nie greifen.
        assertEquals(Ampel.ROT,
                w.bewerte(new BigDecimal("-2000.00"), new BigDecimal("-10000.00")));
    }

    @Test
    void abweichungVonNullIstImmerGruen() {
        assertEquals(Ampel.GRUEN,
                w.bewerte(BigDecimal.ZERO, new BigDecimal("165000.00")));
    }

    @Test
    void gruenWennBeideSchwellenUnterschritten() {
        // 300 EUR bei 165.000 Plan = 0,18 % - beides unter der Schwelle.
        assertEquals(Ampel.GRUEN,
                w.bewerte(new BigDecimal("-300.00"), new BigDecimal("165000.00")));
    }

    @Test
    void gelbWennNurEuroSchwelleGerissen() {
        // Das ist Produkt A: -716,17 bei 165.000 Plan = -0,43 %.
        // Absolut auffaellig, relativ unauffaellig - genau dafuer gibt es zwei Kriterien.
        assertEquals(Ampel.GELB,
                w.bewerte(new BigDecimal("-716.17"), new BigDecimal("165000.00")));
    }

    @Test
    void gelbWennNurProzentSchwelleGerissen() {
        // Kleines Produkt: 400 EUR Abweichung bei 2.000 Plan = 20 %.
        // Euro-Schwelle nicht gerissen, Prozent deutlich.
        assertEquals(Ampel.GELB,
                w.bewerte(new BigDecimal("-400.00"), new BigDecimal("2000.00")));
    }

    @Test
    void rotWennBeideGerissenUndNegativ() {
        // Das ist Produkt B: -16.175,64 bei 162.000 Plan = -9,99 %.
        assertEquals(Ampel.ROT,
                w.bewerte(new BigDecimal("-16175.64"), new BigDecimal("162000.00")));
    }

    @Test
    void gelbWennBeideGerissenAberPositiv() {
        // Das ist Produkt C: +21.070,54 bei 264.000 Plan = +7,98 %.
        // Nicht gruen: Ein unerwarteter Gewinn ist genauso erklaerungsbeduerftig
        // wie ein Verlust. Nicht rot: Es besteht kein Handlungsbedarf.
        assertEquals(Ampel.GELB,
                w.bewerte(new BigDecimal("21070.54"), new BigDecimal("264000.00")));

        // Und Produkt D mit +448 %: ebenfalls gelb. Dass hier ein Datenfehler
        // steckt, meldet bereits die V08-Warnung - die Ampel muss es nicht wiederholen.
        assertEquals(Ampel.GELB,
                w.bewerte(new BigDecimal("726152.92"), new BigDecimal("162000.00")));
    }

    @Test
    void ohnePlanwertZaehltNurDieEuroSchwelle() {
        // Planwert 0: kein sinnvoller Prozentsatz, keine Division durch null.
        assertEquals(Ampel.GELB,
                w.bewerte(new BigDecimal("900.00"), BigDecimal.ZERO));
        assertEquals(Ampel.GRUEN,
                w.bewerte(new BigDecimal("100.00"), BigDecimal.ZERO));
    }

    @Test
    void genauAufDerSchwelleIstNochGruen() {
        // "ueberschritten" heisst echt groesser. 500,00 EUR reissen die 500er-Schwelle
        // nicht - sonst haette man staendig Grenzfaelle, die je nach Rundung kippen.
        assertEquals(Ampel.GRUEN,
                w.bewerte(new BigDecimal("500.00"), new BigDecimal("165000.00")));
    }

    @Test
    void schwellenSindKonfigurierbar() {
        // Ein strengerer Massstab: 100 EUR und 1 %.
        Wesentlichkeit streng = new Wesentlichkeit(
                new BigDecimal("100.00"), new BigDecimal("0.01"));

        // Produkt A waere damit rot statt gelb: 716,17 > 100 und 0,43 % > ... nein,
        // 0,43 % ist immer noch unter 1 % -> bleibt gelb.
        assertEquals(Ampel.GELB,
                streng.bewerte(new BigDecimal("-716.17"), new BigDecimal("165000.00")));
    }

    /**
     * Die abgeleitete Euro-Schwelle skaliert mit dem Datensatz.
     *
     * <p>Der Anlass: Die feste Vorgabe von 500 EUR war an 48 Zeilen kalibriert. Auf 1.810
     * Zeilen mit sechsstelligen Jahres-Deckungsbeitraegen reisst jede Abweichung diese
     * Schwelle - 13 von 15 Produkten wurden gelb, und die Ampel sagte nichts mehr.
     */
    @Test
    void leitetDieEuroSchwelleAusDemDatenumfangAb() {
        Wesentlichkeit gross = Wesentlichkeit.fuer(
                new BigDecimal("3404380.00"), new BigDecimal("0.05"));

        assertEquals(new BigDecimal("8510.95"), gross.schwelleEuro());
        assertEquals(new BigDecimal("0.05"), gross.schwelleProzent(),
                "die Prozent-Schwelle wird unveraendert uebernommen");
    }

    /**
     * Bei einem kleinen Plan faellt die abgeleitete Schwelle unter die Untergrenze -
     * dann gilt die Untergrenze. Ohne sie waere die Schwelle bei einem Plan nahe null
     * praktisch null, und jede Rundungsdifferenz wuerde gemeldet.
     */
    @Test
    void haeltDieUntergrenzeVonFuenfhundert() {
        Wesentlichkeit klein = Wesentlichkeit.fuer(
                new BigDecimal("1000.00"), new BigDecimal("0.05"));

        assertEquals(new BigDecimal("500.00"), klein.schwelleEuro());
    }

    /**
     * Ein Unternehmen, das planmaessig Verlust macht, hat einen negativen Plan-DB-II.
     * Ohne abs() waere die abgeleitete Schwelle negativ - und dann reisst sie JEDE
     * Abweichung, weil ein Betrag nie kleiner als eine negative Zahl ist.
     */
    @Test
    void negativerPlanErgibtDieselbeSchwelleWieDerPositive() {
        assertEquals(
                Wesentlichkeit.fuer(new BigDecimal("3404380.00"), new BigDecimal("0.05")),
                Wesentlichkeit.fuer(new BigDecimal("-3404380.00"), new BigDecimal("0.05")));
    }
}
