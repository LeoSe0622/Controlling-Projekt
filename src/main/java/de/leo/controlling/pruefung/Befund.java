package de.leo.controlling.pruefung;

/**
 * Ein einzelner Prueffund: was ist wo kaputt, und was stand dort tatsaechlich.
 *
 * @param zeilennummer wo in der CSV (1-basiert wie im Editor)
 * @param feld         welche Spalte, z.B. "istMenge"
 * @param regelId      welche Regel angeschlagen hat, z.B. "V02"
 * @param grad         FEHLER oder WARNUNG
 * @param originalwert was tatsaechlich in der Datei stand — ohne ihn ist der Befund
 *                     nicht reparierbar, ohne die CSV zu oeffnen
 * @param meldung      Klartext fuer den Bericht
 */
public record Befund(
        int zeilennummer,
        String feld,
        String regelId,
        Schweregrad grad,
        String originalwert,
        String meldung
) {
}
