package de.leo.controlling.pruefung;

import de.leo.controlling.model.Rohzeile;

import java.util.List;

/**
 * Das Ergebnis der Validierung: was ist kaputt, und womit darf gerechnet werden.
 *
 * <p>Die beiden Listen sind bewusst getrennt. {@code befunde} ist der Bericht - er enthaelt
 * ALLES, auch zu Zeilen, die weiterverwendet werden. {@code verwertbareZeilen} ist die
 * Arbeitsgrundlage fuer die Deckungsbeitragsrechnung.
 *
 * <p>Die Trennlinie ist der Schweregrad: Eine Zeile mit FEHLER fliegt raus, eine Zeile mit
 * WARNUNG bleibt drin. Ein Ausreisser wird also mitgerechnet und verzerrt die Kennzahlen
 * sichtbar - das ist gewollt.
 *
 * @param befunde           alle Befunde, nach Zeilennummer sortiert
 * @param verwertbareZeilen alle Zeilen ohne FEHLER-Befund
 * @param gepruefteZeilen   wie viele Zeilen insgesamt geprueft wurden
 * @param regeln            eine Zaehlung je Regel, auch fuer Regeln ohne Befund
 */
public record Pruefprotokoll(
        List<Befund> befunde,
        List<Rohzeile> verwertbareZeilen,
        int gepruefteZeilen,
        List<Regelzaehlung> regeln
) {

    /** Wie viele Befunde den Grad FEHLER haben. */
    public long anzahlFehler() {
        return befunde.stream().filter(b -> b.grad() == Schweregrad.FEHLER).count();
    }

    /** Wie viele Befunde den Grad WARNUNG haben. */
    public long anzahlWarnungen() {
        return befunde.stream().filter(b -> b.grad() == Schweregrad.WARNUNG).count();
    }

    /**
     * Anteil der verwertbaren Zeilen, zwischen 0.0 und 1.0.
     * Bei 43 verwertbaren von 49 geprueften Zeilen also 0,878.
     */
    public double qualitaetsquote() {

        if (gepruefteZeilen == 0) {
            return 1.0;
        }

        return (double) verwertbareZeilen.size() / gepruefteZeilen;
    }
}
