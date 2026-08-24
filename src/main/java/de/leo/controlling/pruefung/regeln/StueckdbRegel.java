package de.leo.controlling.pruefung.regeln;

import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Schweregrad;
import de.leo.controlling.pruefung.Zeilenregel;
import de.leo.controlling.util.Zahlen;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * V06 — die variablen Stueckkosten sind hoeher als der Preis.
 *
 * <p>Dann ist der Deckungsbeitrag je Stueck negativ: Jedes verkaufte Stueck macht Verlust,
 * und mehr Absatz macht das Ergebnis schlechter statt besser. Fachlich moeglich (Verdraengungs-
 * preis, Fehlkalkulation), aber immer erklaerungsbeduerftig — deshalb WARNUNG, nicht FEHLER:
 * Man KANN damit rechnen, man sollte es nur sehen.
 *
 * <p>Auf sauber kalkulierten Daten feuert sie nie - dort liegt jeder Preis ueber den
 * variablen Stueckkosten, etwa 50 zu 30 oder 120 zu 70.
 */
public class StueckdbRegel implements Zeilenregel {

    private static final String ID = "V06";

    @Override
    public List<Befund> pruefe(Rohzeile zeile) {
        List<Befund> befunde = new ArrayList<>();

        pruefePreis(zeile, "planPreis", zeile.planPreis(), zeile.variableStueckkosten(), befunde);
        pruefePreis(zeile, "istPreis", zeile.istPreis(), zeile.variableStueckkosten(), befunde);

        return befunde;
    }

    /**
     * @param preisFeld  Name der Preisspalte fuer den Bericht ("planPreis" / "istPreis")
     * @param preisWert  Rohwert des Preises
     * @param kostenWert Rohwert der variablen Stueckkosten
     */
    private static void pruefePreis(Rohzeile zeile, String preisFeld, String preisWert,
                                    String kostenWert, List<Befund> befunde) {

        BigDecimal preis = Zahlen.parse(preisWert);
        BigDecimal kosten = Zahlen.parse(kostenWert);
        if (preis == null || kosten == null) {
            return;
        }

        if (preis.signum() < 0 || kosten.signum() < 0) {
            return;
        }

        if (kosten.compareTo(preis) >= 0) {
            befunde.add(new Befund(
                    zeile.zeilennummer(),
                    preisFeld,
                    ID,
                    Schweregrad.WARNUNG,
                    preisWert,
                    "Variable Stueckkosten (" + kostenWert + ") sind nicht kleiner als "
                            + preisFeld + " (" + preisWert + ") - Deckungsbeitrag je Stueck ist negativ"
            ));
        }
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String bezeichnung() {
        return "Stueck-DB negativ";
    }
}
