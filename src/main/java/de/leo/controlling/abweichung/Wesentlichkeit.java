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
 * <p>Produkt A ist der Lehrfall: 716,17 EUR Abweichung reissen die Euro-Schwelle, sind aber
 * nur 0,43 % vom Plan. Absolut auffaellig, relativ unauffaellig — also gelb, nicht rot.
 *
 * @param schwelleEuro    absolute Schwelle, Standard 500,00
 * @param schwelleProzent relative Schwelle als Anteil (0.05 = 5 %)
 */
public record Wesentlichkeit(BigDecimal schwelleEuro, BigDecimal schwelleProzent) {

    /** Die uebliche Einstellung: 500 EUR und 5 %. */
    public static Wesentlichkeit standard() {
        return new Wesentlichkeit(new BigDecimal("500.00"), new BigDecimal("0.05"));
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
     * ist genauso erklaerungsbeduerftig wie ein unerwarteter Verlust — und oft genug ein
     * Datenfehler.
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

        if(euroUeberschritten && prozentUeberschritten && abweichung.signum() < 0) {
            return Ampel.ROT;
        }
        if(euroUeberschritten || prozentUeberschritten) {
            return Ampel.GELB;
        } else {
            return Ampel.GRUEN;
        }
    }
}
