package de.leo.controlling.pruefung.regeln;

import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Schweregrad;
import de.leo.controlling.pruefung.Zeilenregel;

import java.util.List;

/**
 * V01 — die Zeile hat nicht 9 Spalten.
 *
 * <p>Der CsvEinleser fuellt fehlende Spalten mit "" auf, damit nichts abstuerzt. Genau
 * deshalb braucht es diese Regel: Ohne sie waere eine Zeile mit 7 Spalten nicht mehr von
 * einer Zeile mit 9 Spalten zu unterscheiden, bei der zwei Felder leer sind.
 *
 * <p>Auf sauber erzeugten Daten feuert sie nie - dort hat jede Zeile 9 Spalten.
 */
public class SpaltenanzahlRegel implements Zeilenregel {

    private static final String ID = "V01";
    private static final int ERWARTETE_SPALTEN = 9;

    @Override
    public List<Befund> pruefe(Rohzeile zeile) {

        if (zeile.spaltenAnzahl() == ERWARTETE_SPALTEN) {
            return List.of();
        }

        Befund befund = new Befund(
                zeile.zeilennummer(),
                "(ganze Zeile)",
                ID,
                Schweregrad.FEHLER,
                String.valueOf(zeile.spaltenAnzahl()),
                "Zeile hat " + zeile.spaltenAnzahl() + " Spalten, erwartet werden " + ERWARTETE_SPALTEN
        );

        return List.of(befund);
    }
}
