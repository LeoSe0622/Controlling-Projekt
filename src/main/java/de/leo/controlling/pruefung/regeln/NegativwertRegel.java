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
 * V05 — ein Zahlenfeld ist negativ.
 *
 * <p>Mengen, Preise und Kosten koennen fachlich nicht negativ sein. Ein negativer Wert
 * ist unbrauchbar, nicht nur verdaechtig — deshalb FEHLER, nicht WARNUNG.
 *
 * <p>Schweigegrundsatz: schweigt bei leeren Feldern (V02) UND bei unparsebaren (V03).
 * Diese Regel ist die dritte Stufe und setzt voraus, dass die beiden davor ihre Arbeit tun.
 *
 * <p>Auf den echten Daten feuert sie einmal: Zeile 14, istPreis = -77.05.
 */
public class NegativwertRegel implements Zeilenregel {

    private static final String ID = "V05";

    @Override
    public List<Befund> pruefe(Rohzeile zeile) {
        List<Befund> befunde = new ArrayList<>();

        pruefeFeld(zeile, "planMenge", zeile.planMenge(), befunde);
        pruefeFeld(zeile, "istMenge", zeile.istMenge(), befunde);
        pruefeFeld(zeile, "planPreis", zeile.planPreis(), befunde);
        pruefeFeld(zeile, "istPreis", zeile.istPreis(), befunde);
        pruefeFeld(zeile, "variableStueckkosten", zeile.variableStueckkosten(), befunde);
        pruefeFeld(zeile, "fixkostenProdukt", zeile.fixkostenProdukt(), befunde);

        return befunde;
    }

    private static void pruefeFeld(Rohzeile zeile, String feldname, String wert, List<Befund> befunde) {

        BigDecimal zahl = Zahlen.parse(wert);
        if (zahl == null) {
            return;
        }

        if (zahl.signum() < 0) {
            befunde.add(new Befund(
                    zeile.zeilennummer(),
                    feldname,
                    ID,
                    Schweregrad.FEHLER,
                    wert,
                    "Feld '" + feldname + "' ist negativ (" + wert + ")"
            ));
        }
    }
}
