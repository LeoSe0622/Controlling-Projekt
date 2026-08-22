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
 * <p>Auf den echten Daten feuert sie nie — alle vier Produkte haben eine gesunde Marge
 * (A: 30 zu 50, B: 45 zu 80, C: 12 zu 25, D: 70 zu 120).
 */
public class StueckdbRegel implements Zeilenregel {

    private static final String ID = "V06";

    @Override
    public List<Befund> pruefe(Rohzeile zeile) {
        List<Befund> befunde = new ArrayList<>();

        // Die variablen Stueckkosten gelten laut CSV fuer beide Seiten,
        // also gegen beide Preise pruefen.
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

        // Zwei Werte -> beide muessen brauchbar sein, bevor verglichen werden darf.
        BigDecimal preis = Zahlen.parse(preisWert);
        BigDecimal kosten = Zahlen.parse(kostenWert);
        if (preis == null || kosten == null) {
            return;
        }

        // Schweigegrundsatz: Negatives ist V05s Thema. Ohne das wuerde Zeile 14
        // (istPreis = -77.05) hier ein zweites Mal auftauchen, denn 45 >= -77.05.
        if (preis.signum() < 0 || kosten.signum() < 0) {
            return;
        }

        if (kosten.compareTo(preis) >= 0) {
            befunde.add(new Befund(
                    zeile.zeilennummer(),
                    preisFeld,
                    ID,
                    Schweregrad.Grad.Warnung,
                    preisWert,
                    "Variable Stueckkosten (" + kostenWert + ") sind nicht kleiner als "
                            + preisFeld + " (" + preisWert + ") - Deckungsbeitrag je Stueck ist negativ"
            ));
        }
    }
}
