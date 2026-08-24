package de.leo.controlling.pruefung.regeln;

import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Datensatzregel;
import de.leo.controlling.pruefung.Schweregrad;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * V07 — dieselbe Kombination aus Monat, Produkt und Kostenstelle kommt mehrfach vor.
 *
 * <p>FEHLER, und zwar fuer <b>alle</b> beteiligten Zeilen: Welche die richtige ist, kann
 * das Programm nicht wissen. Wuerde man eine behalten, waere die Wahl willkuerlich.
 *
 * <p>Warum das mehr ist als Ordnungsliebe: Die fixkosten_produkt stehen auf JEDER Zeile.
 * Ein Produkt mit einer Dublette hat eine Monatszeile zu viel — wer die Fixkosten zeilenweise
 * aufsummiert, bekommt 65.000 statt 60.000. Plausibel falsch, und deshalb gefaehrlich.
 *
 * <p>Sie meldet immer BEIDE beteiligten Zeilen, nie nur die zweite.
 */
public class DublettenRegel implements Datensatzregel {

    private static final String ID = "V07";

    @Override
    public List<Befund> pruefe(List<Rohzeile> alle) {
        List<Befund> befunde = new ArrayList<>();

        Map<String, List<Rohzeile>> gruppen = new LinkedHashMap<>();
        for (Rohzeile z : alle) {
            gruppen.computeIfAbsent(schluessel(z), k -> new ArrayList<>()).add(z);
        }

        for (List<Rohzeile> gruppe : gruppen.values()) {
            if (gruppe.size() < 2) {
                continue;
            }

            for (Rohzeile z : gruppe) {

                String andere = gruppe.stream()
                        .filter(g -> g.zeilennummer() != z.zeilennummer())
                        .map(g -> String.valueOf(g.zeilennummer()))
                        .collect(Collectors.joining(", "));

                befunde.add(new Befund(
                        z.zeilennummer(),
                        "(ganze Zeile)",
                        ID,
                        Schweregrad.FEHLER,
                        beschriftung(z),
                        "Dublette: gleiche Kombination wie Zeile " + andere
                ));
            }
        }

        return befunde;
    }

    /**
     * Der fachliche Schluessel einer Zeile.
     *
     * <p>Warum nicht einfach {@code zeile.equals(andere)}: Die {@code zeilennummer()} ist
     * Teil des Records, also vergleicht das generierte equals() sie mit. Zwei inhaltlich
     * identische Zeilen aus verschiedenen Dateizeilen sind fuer {@code equals} verschieden.
     */
    private static String schluessel(Rohzeile zeile) {

        return zeile.monat() + "|" + zeile.produkt() + "|" + zeile.kostenstelle();
    }

    /**
     * Dieselbe Kombination, aber fuer Menschen.
     *
     * <p>Getrennt von {@link #schluessel}, weil beide verschiedene Zwecke haben: Der
     * Schluessel muss eindeutig sein, die Beschriftung lesbar. Wer beides in eine Methode
     * legt, bekommt am Ende ein Trennzeichen im Bericht, das nur der Gruppierung dient -
     * das Pipe-Zeichen ist ein Implementierungsdetail und hat in einer Berichtsspalte
     * nichts zu suchen.
     */
    private static String beschriftung(Rohzeile zeile) {

        return zeile.monat() + " / " + zeile.produkt() + " / " + zeile.kostenstelle();
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String bezeichnung() {
        return "Dublette";
    }
}
