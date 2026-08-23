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
     * Fuehrt alle Regeln aus und trennt verwertbare von unbrauchbaren Zeilen.
     *
     * <p>Aussen die Zeilen, innen die Regeln: So kommen die Befunde nach Zeile sortiert
     * heraus statt nach Regel - fuer den Bericht die nuetzlichere Reihenfolge.
     *
     * <p>Die Fehlerzeilen landen in einem {@code Set}, nicht in einer {@code List}: Eine
     * Zeile kann mehrere Fehler haben, und {@code contains()} ist beim Set unabhaengig
     * von der Groesse schnell.
     *
     * @param alle alle eingelesenen Rohzeilen
     * @return Befunde und verwertbare Zeilen
     */
    public Pruefprotokoll pruefe(List<Rohzeile> alle) {
        List<Befund> befunde = new ArrayList<>();

        for (Rohzeile zeile : alle) {
            for (Zeilenregel regel : zeilenregeln) {
                befunde.addAll(regel.pruefe(zeile));
            }
        }

        for (Datensatzregel regel : datensatzregeln) {
            befunde.addAll(regel.pruefe(alle));
        }

        Set<Integer> fehlerZeilen = befunde.stream()
                .filter(b -> b.grad() == Schweregrad.FEHLER)
                .map(Befund::zeilennummer)
                .collect(Collectors.toSet());

        List<Rohzeile> verwertbar = alle.stream()
                .filter(z -> !fehlerZeilen.contains(z.zeilennummer()))
                .toList();

        return new Pruefprotokoll(befunde, verwertbar, alle.size());
    }
}
