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
import de.leo.controlling.pruefung.regeln.ZukunftsmonatRegel;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fuehrt alle Regeln aus und fasst das Ergebnis in einem {@link Pruefprotokoll} zusammen.
 *
 * <p>Der Validator kennt keine einzige Pruefung selbst - er kennt nur zwei Listen von
 * Regeln und laesst sie laufen. Genau das war der Sinn der Aufteilung in Regelklassen:
 * Eine neue Regel kostet eine neue Klasse und eine Zeile in der Registry unten. Nichts
 * Bestehendes wird angefasst, und keine Methode waechst mit.
 */
public class Validator {

    private final List<Zeilenregel> zeilenregeln;
    private final List<Datensatzregel> datensatzregeln;

    /** Mit dem heutigen Monat als Bezugspunkt fuer V09. */
    public Validator() {
        this(YearMonth.now());
    }

    /**
     * @param berichtsmonat Bezugspunkt fuer V09 - alles danach gilt als Zukunft.
     *                      Als Parameter, damit Tests nicht vom Kalender abhaengen.
     */
    public Validator(YearMonth berichtsmonat) {

        this.zeilenregeln = List.of(
                new SpaltenanzahlRegel(),
                new PflichtfeldRegel(),
                new ZahlformatRegel(),
                new MonatsformatRegel(),
                new NegativwertRegel(),
                new StueckdbRegel(),
                new AusreisserRegel(),
                new ZukunftsmonatRegel(berichtsmonat)
        );

        this.datensatzregeln = List.of(new DublettenRegel());
    }

    /**
     * Fuehrt alle Regeln aus und trennt verwertbare von unbrauchbaren Zeilen.
     *
     * <p>Am Ende wird nach Zeilennummer sortiert, und zwar STABIL. Ohne diesen Schritt
     * haengen die Befunde der Datensatzregeln hinten an: Der Bericht zeigt erst alle
     * Zeilenbefunde aufsteigend, faengt dann wieder von vorn an, und wer wissen will, was
     * mit Zeile 456 los ist, muss an zwei Stellen suchen. Weil {@code List.sort} stabil
     * ist, bleibt innerhalb einer Zeile die Regelreihenfolge erhalten - die Ursache steht
     * also weiterhin vor ihren Folgen.
     *
     * <p>Die Fehlerzeilen landen in einem {@code Set}, nicht in einer {@code List}: Eine
     * Zeile kann mehrere Fehler haben, und {@code contains()} ist beim Set unabhaengig
     * von der Groesse schnell.
     *
     * @param alle alle eingelesenen Rohzeilen
     * @return Befunde nach Zeilennummer sortiert, dazu die Zeilen ohne FEHLER
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

        befunde.sort(Comparator.comparingInt(Befund::zeilennummer));

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
