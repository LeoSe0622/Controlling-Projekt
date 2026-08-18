package de.leo.controlling.pruefung.regeln;

import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Schweregrad;
import de.leo.controlling.pruefung.Zeilenregel;

import java.util.ArrayList;
import java.util.List;

/**
 * V02 — Pflichtfeld leer.
 *
 * <p>Alle neun Spalten der CSV sind Pflicht. Ein leeres Feld ist ein FEHLER: die Zeile
 * fliegt aus der Rechnung. Trifft in den echten Daten die Zeilen 22, 30 und 32
 * (jeweils {@code istMenge}).
 *
 * <p>Diese Regel ist die <b>erste Stufe</b> des Schweigegrundsatzes: Weil sie leere Felder
 * meldet, dürfen V03 (Zahlformat) und V05 (Negativwerte) bei leeren Feldern schweigen.
 */
public class PflichtfeldRegel implements Zeilenregel {

    private static final String ID = "V02";

    @Override
    public List<Befund> pruefe(Rohzeile zeile) {
        List<Befund> befunde = new ArrayList<>();

        pruefeFeld(zeile, "monat", zeile.monat(), befunde);
        pruefeFeld(zeile, "produkt", zeile.produkt(), befunde);
        pruefeFeld(zeile, "kostenstelle",zeile.kostenstelle() , befunde);
        pruefeFeld(zeile, "planMenge", zeile.planMenge(), befunde);
        pruefeFeld(zeile, "istMenge", zeile.istMenge(), befunde);
        pruefeFeld(zeile, "planPreis", zeile.planPreis(), befunde);
        pruefeFeld(zeile,"istPreis" , zeile.istPreis(), befunde);
        pruefeFeld(zeile, "variableStueckkosten", zeile.variableStueckkosten(), befunde);
        pruefeFeld(zeile, "fixkostenProdukt", zeile.fixkostenProdukt(), befunde);
        
        
        
        return befunde;
    }

    /**
     * Legt einen Befund in die Liste, wenn der Wert leer ist. Sonst passiert nichts.
     *
     * @param feldname der Name fuer den Bericht, z.B. "istMenge"
     * @param wert     der Rohwert aus der CSV
     */
    private static void pruefeFeld(Rohzeile zeile, String feldname, String wert, List<Befund> befunde) {

        if (wert== null || wert.isEmpty()) {
        befunde.add(new Befund(
            zeile.zeilennummer(),
            feldname,
            ID,
            Schweregrad.Grad.Fehler,
            wert,
            "Pflichtfeld:" + feldname + "ist leer"
        ));    
        }
        
    }
}
