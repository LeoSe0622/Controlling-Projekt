package de.leo.controlling.pruefung.regeln;

import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Schweregrad;
import de.leo.controlling.pruefung.Zeilenregel;
import de.leo.controlling.util.Zahlen;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * V08 — der Ist-Wert weicht um mehr als 50 % vom Plan ab.
 *
 * <p>WARNUNG, nicht FEHLER: Der Wert IST eine Zahl, man KANN damit rechnen — er ist nur
 * unglaubwuerdig. Die Zeile bleibt in der Rechnung und verzerrt die Kennzahlen sichtbar.
 * Genau das soll ein Controller sehen, statt eine stillschweigend geglaettete Zahl.
 *
 * <p>Auf den echten Daten feuert sie einmal: Zeile 46, istMenge = 17950 bei Plan 350.
 */
public class AusreisserRegel implements Zeilenregel {

    private static final String ID = "V08";

    /** Ab welcher relativen Abweichung gewarnt wird: 0.50 = 50 %. */
    private static final BigDecimal SCHWELLE = new BigDecimal("0.50");

    /** Nachkommastellen der Abweichung: 4 Stellen = 0,01 Prozentpunkte. */
    private static final int SKALA = 4;

    @Override
    public List<Befund> pruefe(Rohzeile zeile) {
        List<Befund> befunde = new ArrayList<>();

        // Gemeldet wird immer das IST-Feld - das ist der verdaechtige Wert.
        pruefePaar(zeile, "istMenge", zeile.planMenge(), zeile.istMenge(), befunde);
        pruefePaar(zeile, "istPreis", zeile.planPreis(), zeile.istPreis(), befunde);

        return befunde;
    }

    /**
     * @param istFeld  Name des Ist-Feldes fuer den Bericht ("istMenge" / "istPreis")
     * @param planWert Rohwert der Plan-Seite
     * @param istWert  Rohwert der Ist-Seite
     */
    private static void pruefePaar(Rohzeile zeile, String istFeld, String planWert,
                                   String istWert, List<Befund> befunde) {

        BigDecimal plan = Zahlen.parse(planWert);
        BigDecimal ist = Zahlen.parse(istWert);
        if (plan == null || ist == null) {
            return;
        }

        // Schweigegrundsatz: Negatives gehoert V05. Ohne das waere Zeile 14
        // (istPreis = -77.05, 196 % Abweichung) doppelt gemeldet.
        if (plan.signum() < 0 || ist.signum() < 0) {
            return;
        }

        // Ohne Plan-Wert gibt es keine sinnvolle relative Abweichung - und die
        // Division wuerde eine ArithmeticException werfen.
        if (plan.signum() == 0) {
            return;
        }

        // divide() braucht bei BigDecimal Skala UND Rundungsmodus: Ist das Ergebnis
        // nicht exakt darstellbar (z.B. 1/3), wirft es sonst. BigDecimal rundet nie
        // heimlich - es verlangt, dass wir die Genauigkeit festlegen.
        BigDecimal abweichung = ist.subtract(plan).abs()
                .divide(plan, SKALA, RoundingMode.HALF_UP);

        if (abweichung.compareTo(SCHWELLE) > 0) {
            BigDecimal prozent = abweichung.multiply(new BigDecimal("100"))
                    .setScale(1, RoundingMode.HALF_UP);

            befunde.add(new Befund(
                    zeile.zeilennummer(),
                    istFeld,
                    ID,
                    Schweregrad.Grad.Warnung,
                    istWert,
                    istFeld + " " + istWert + " weicht " + prozent + " % von Plan "
                            + planWert + " ab"
            ));
        }
    }
}
