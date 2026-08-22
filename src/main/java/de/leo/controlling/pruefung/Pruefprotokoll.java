package de.leo.controlling.pruefung;

import de.leo.controlling.model.Rohzeile;

import java.util.List;

/**
 * Das Ergebnis der Validierung: was ist kaputt, und womit darf gerechnet werden.
 *
 * <p>Die beiden Listen sind bewusst getrennt. {@code befunde} ist der Bericht — er enthaelt
 * ALLES, auch zu Zeilen, die weiterverwendet werden. {@code verwertbareZeilen} ist die
 * Arbeitsgrundlage fuer die Deckungsbeitragsrechnung.
 *
 * <p>Die Trennlinie ist der Schweregrad: Eine Zeile mit FEHLER fliegt raus, eine Zeile mit
 * WARNUNG bleibt drin. Zeile 46 (der Ausreisser mit 17950 statt 350) wird also mitgerechnet
 * und verzerrt die Kennzahlen sichtbar — das ist gewollt.
 *
 * @param befunde           alle Befunde, in der Reihenfolge, in der sie gefunden wurden
 * @param verwertbareZeilen alle Zeilen ohne FEHLER-Befund
 * @param gepruefteZeilen   wie viele Zeilen insgesamt geprueft wurden
 */
public record Pruefprotokoll(
        List<Befund> befunde,
        List<Rohzeile> verwertbareZeilen,
        int gepruefteZeilen
) {

    // Ein record darf Methoden haben, die etwas AUSRECHNEN - nur zusaetzlichen
    // Zustand darf er nicht haben. Diese drei Werte leiten sich vollstaendig aus
    // den Komponenten ab, also gehoeren sie hierher und nicht in den Konstruktor:
    // sonst koennte jemand ein Protokoll bauen, dessen Quote nicht zu den Listen passt.

    /** Wie viele Befunde den Grad FEHLER haben. */
    public long anzahlFehler() {
        return befunde.stream().filter(b -> b.grad() == Schweregrad.Grad.Fehler).count();
    }

    /** Wie viele Befunde den Grad WARNUNG haben. */
    public long anzahlWarnungen() {
        return befunde.stream().filter(b -> b.grad() == Schweregrad.Grad.Warnung).count();
    }

    /**
     * Anteil der verwertbaren Zeilen, zwischen 0.0 und 1.0.
     * Bei 43 von 49 Zeilen also 0.878.
     */
    public double qualitaetsquote() {

        // Nur gepruefteZeilen steht im Nenner - nur die kann die Division sprengen.
        // Bei einer leeren Datei ist 1.0 die sinnvollere Antwort als ein Absturz:
        // es gibt nichts Kaputtes.
        if (gepruefteZeilen == 0) {
            return 1.0;
        }

        // Der Cast ist Pflicht: int / int ergibt in Java einen int,
        // 43 / 49 waere 0 statt 0.878.
        return (double) verwertbareZeilen.size() / gepruefteZeilen;
    }
}
