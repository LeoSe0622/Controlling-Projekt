package de.leo.controlling.pruefung.regeln;

import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Schweregrad;
import de.leo.controlling.pruefung.Zeilenregel;
import de.leo.controlling.util.Zahlen;

import java.util.ArrayList;
import java.util.List;

/**
 * V03 — ein Zahlenfeld enthaelt keine gueltige Zahl.
 *
 * <p>Betrifft die sechs Zahlenspalten. Schweigegrundsatz: Bei leeren Feldern meldet nur
 * V02 — sonst bekaeme Zeile 22 zwei Befunde fuer dasselbe Problem.
 *
 * <p>Auf den echten Daten feuert sie nie — alle gefuellten Zahlenfelder sind gueltig.
 */
public class ZahlformatRegel implements Zeilenregel {

    private static final String ID = "V03";

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

        if (wert == null || wert.isBlank()) {
            return;
        }

        if (Zahlen.parse(wert) == null) {
            befunde.add(new Befund(
                    zeile.zeilennummer(),
                    feldname,
                    ID,
                    Schweregrad.FEHLER,
                    wert,
                    "Feld '" + feldname + "' ist keine gueltige Zahl"
            ));
        }
    }
}
