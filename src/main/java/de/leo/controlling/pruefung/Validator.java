package de.leo.controlling.pruefung;

import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.regeln.AusreisserRegel;
import de.leo.controlling.pruefung.regeln.DublettenRegel;
import de.leo.controlling.pruefung.regeln.MonatsformatRegel;
import de.leo.controlling.pruefung.regeln.NegativwertRegel;
import de.leo.controlling.pruefung.regeln.PflichtfeldRegel;
import de.leo.controlling.pruefung.regeln.SpaltenanzahlRegel;
import de.leo.controlling.pruefung.regeln.StueckdbRegel;
import de.leo.controlling.pruefung.regeln.ZahlformatRegel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fuehrt alle Regeln aus und fasst das Ergebnis in einem {@link Pruefprotokoll} zusammen.
 *
 * <p>Der Validator kennt keine einzige Pruefung selbst — er kennt nur zwei Listen von
 * Regeln und laesst sie laufen. Genau das war der Sinn der Aufteilung in Regelklassen:
 * Eine neue Regel kostet eine neue Klasse und eine Zeile in der Registry unten. Nichts
 * Bestehendes wird angefasst, und keine Methode waechst mit.
 */
public class Validator {

    private final List<Zeilenregel> zeilenregeln;
    private final List<Datensatzregel> datensatzregeln;

    /** Baut einen Validator mit allen acht Standardregeln. */
    public Validator() {

        this.zeilenregeln = List.of(
                new SpaltenanzahlRegel(),
                new PflichtfeldRegel(),
                new ZahlformatRegel(),
                new MonatsformatRegel(),
                new NegativwertRegel(),
                new StueckdbRegel(),
                new AusreisserRegel()
        );

        this.datensatzregeln = List.of(new DublettenRegel());
    }

    /**
     * @param alle alle eingelesenen Rohzeilen
     * @return Befunde und verwertbare Zeilen
     */
    public Pruefprotokoll pruefe(List<Rohzeile> alle) {
        List<Befund> befunde = new ArrayList<>();

        // Jede Zeilenregel auf jede Zeile. Aussen die Zeilen, damit die Befunde
        // nach Zeile sortiert herauskommen - so liest sich der Bericht besser.
        for (Rohzeile zeile : alle) {
            for (Zeilenregel regel : zeilenregeln) {
                befunde.addAll(regel.pruefe(zeile));
            }
        }

        // Datensatzregeln sehen die gesamte Liste auf einmal.
        for (Datensatzregel regel : datensatzregeln) {
            befunde.addAll(regel.pruefe(alle));
        }

        // Welche Zeilennummern haben mindestens einen FEHLER?
        // Ein Set, keine List: Zeile 22 kann mehrere Fehler haben, und contains()
        // ist beim Set unabhaengig von der Groesse schnell.
        Set<Integer> fehlerZeilen = befunde.stream()
                .filter(b -> b.grad() == Schweregrad.Grad.Fehler)
                .map(Befund::Zeilennummer)
                .collect(Collectors.toSet());

        // Alles ohne FEHLER bleibt verwertbar. Warnungen zaehlen nicht -
        // die Zeile bleibt drin und wird nur markiert.
        List<Rohzeile> verwertbar = alle.stream()
                .filter(z -> !fehlerZeilen.contains(z.zeilennummer()))
                .toList();

        return new Pruefprotokoll(befunde, verwertbar, alle.size());
    }
}
