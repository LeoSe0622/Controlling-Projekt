package de.leo.controlling.abweichung;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ab wann ist eine Abweichung wesentlich? Zwei Schwellen, beide muessen gerissen sein,
 * damit es rot wird.
 *
 * <p><b>Warum zwei Kriterien und nicht eines:</b> Eine reine Euro-Schwelle schlaegt bei
 * grossen Produkten staendig an und bei kleinen nie. Eine reine Prozent-Schwelle macht aus
 * jeder Kleinigkeit ein Drama, sobald der Plan nahe null liegt. Erst zusammen beschreiben
 * sie "wesentlich" brauchbar.
 *
 * <p>Der Lehrfall ist ein grosses Produkt mit kleiner relativer Abweichung: Der Betrag
 * reisst die Euro-Schwelle, liegt aber weit unter der Prozent-Schwelle. Absolut
 * auffaellig, relativ unauffaellig - also gelb, nicht rot.
 *
 * @param schwelleEuro    absolute Schwelle, Standard 500,00
 * @param schwelleProzent relative Schwelle als Anteil (0.05 = 5 %)
 */
public record Wesentlichkeit(BigDecimal schwelleEuro, BigDecimal schwelleProzent) {

    /** Untergrenze der Euro-Schwelle. Darunter meldet die Ampel Rauschen. */
    private static final BigDecimal MINDESTSCHWELLE = new BigDecimal("500.00");

    /** Anteil des Plan-Ergebnisses, ab dem eine Abweichung nicht mehr Rauschen ist. */
    private static final BigDecimal AUTO_ANTEIL = new BigDecimal("0.0025");

    /** Die uebliche Einstellung: 500 EUR und 5 %. */
    public static Wesentlichkeit standard() {
        return new Wesentlichkeit(MINDESTSCHWELLE, new BigDecimal("0.05"));
    }

    /**
     * Leitet die Euro-Schwelle aus dem Umfang des Datensatzes ab.
     *
     * <p><b>Warum das noetig wurde:</b> Die feste Vorgabe von 500 EUR war an einem
     * Datensatz mit 48 Zeilen kalibriert. Auf 1.810 Zeilen mit Jahres-Deckungsbeitraegen
     * im sechsstelligen Bereich reisst JEDE Abweichung diese Schwelle - 13 von 15
     * Produkten wurden gelb, und die Ampel sagte nichts mehr. Die Prozent-Schwelle
     * skaliert von allein mit, die Euro-Schwelle nicht.
     *
     * <p>0,25 % des geplanten Betriebsergebnisses, mindestens aber {@code MINDESTSCHWELLE}:
     * Der Anteil haelt die Schwelle bei grossen Datensaetzen aussagekraeftig, die
     * Untergrenze verhindert, dass sie bei einem Plan nahe null auf nichts zusammenfaellt.
     *
     * @param planGesamt      das geplante Betriebsergebnis (Plan-DB II ueber alle Produkte)
     * @param schwelleProzent die relative Schwelle, unveraendert uebernommen
     */
    public static Wesentlichkeit fuer(BigDecimal planGesamt, BigDecimal schwelleProzent) {

        BigDecimal abgeleitet = planGesamt.abs()
                .multiply(AUTO_ANTEIL)
                .setScale(2, RoundingMode.HALF_UP);

        return new Wesentlichkeit(abgeleitet.max(MINDESTSCHWELLE), schwelleProzent);
    }

    /**
     * Bewertet eine Abweichung.
     *
     * <pre>
     *   ROT    beide Schwellen ueberschritten UND negativ
     *   GELB   mindestens eine Schwelle ueberschritten
     *   GRUEN  keine Schwelle ueberschritten
     * </pre>
     *
     * <p>Grosse POSITIVE Abweichungen werden bewusst nicht gruen: Ein unerwarteter Gewinn
     * ist genauso erklaerungsbeduerftig wie ein unerwarteter Verlust - und oft genug ein
     * Datenfehler.
     *
     * <p>Die Reihenfolge der Pruefung ist nicht beliebig: ROT muss zuerst stehen, sonst
     * faengt die GELB-Bedingung mit ihrem ODER saemtliche roten Faelle vorher ab - der
     * Code uebersetzt, fast alle Tests bleiben gruen, und der Bericht meldet nie
     * Handlungsbedarf.
     *
     * <p>Beim Prozentkriterium steht {@code abs()} auf BEIDEN Seiten der Division. Bei
     * einem negativen Planwert - einem Produkt, das planmaessig Verlust macht - waere der
     * Quotient sonst negativ und der Vergleich immer falsch; die Prozent-Schwelle wuerde
     * stillschweigend nie greifen.
     *
     * @param abweichung Ist minus Plan; "+" heisst ergebnisverbessernd
     * @param planwert   Bezugsgroesse fuer den Prozentsatz (der Plan-DB II)
     */
    public Ampel bewerte(BigDecimal abweichung, BigDecimal planwert) {

        boolean euroUeberschritten = abweichung.abs().compareTo(schwelleEuro) > 0;

        boolean prozentUeberschritten = false;
        if (planwert.signum() != 0) {
            BigDecimal anteil = abweichung.abs()
                    .divide(planwert.abs(), 4, RoundingMode.HALF_UP);
            prozentUeberschritten = anteil.compareTo(schwelleProzent) > 0;
        }

        if (euroUeberschritten && prozentUeberschritten && abweichung.signum() < 0) {
            return Ampel.ROT;
        }
        if (euroUeberschritten || prozentUeberschritten) {
            return Ampel.GELB;
        }
        return Ampel.GRUEN;
    }
}
