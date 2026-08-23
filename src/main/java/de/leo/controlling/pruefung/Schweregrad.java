package de.leo.controlling.pruefung;

/**
 * Wie schwer wiegt ein Befund.
 *
 * <p>Die Unterscheidung entscheidet, was mit der Zeile passiert — nicht nur, wie sie
 * im Bericht aussieht.
 *
 * <p>Faustregel bei einer neuen Regel: <i>Kann ich mit dem Wert rechnen?</i>
 * Nein → FEHLER. Ja, aber ich sollte ihn nicht glauben → WARNUNG.
 */
public enum Schweregrad {

    /** Wert unbrauchbar: Die Zeile fliegt aus der Rechnung, erscheint aber im Bericht. */
    FEHLER,

    /** Verdächtig, aber rechenbar: Die Zeile bleibt drin und wird markiert. */
    WARNUNG
}
