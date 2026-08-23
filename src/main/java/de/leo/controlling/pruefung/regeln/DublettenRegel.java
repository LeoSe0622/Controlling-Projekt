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
 * Produkt A hat wegen der Dublette 13 Zeilen statt 12 — wer die Fixkosten zeilenweise
 * aufsummiert, bekommt 65.000 statt 60.000. Plausibel falsch, und deshalb gefaehrlich.
 *
 * <p>Auf den echten Daten feuert sie zweimal: Zeile 6 und Zeile 21
 * (beide 2025-04, Produkt A, Vertrieb Nord).
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
                        schluessel(z),
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
     * Teil des Records, also vergleicht das generierte equals() sie mit. Zeile 6 und
     * Zeile 21 sind inhaltlich identisch, aber {@code equals} sagt trotzdem false.
     */
    private static String schluessel(Rohzeile zeile) {

        return zeile.monat() + "|" + zeile.produkt() + "|" + zeile.kostenstelle();
    }
}
