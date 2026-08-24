package de.leo.controlling.pruefung;

/**
 * Wie viele Befunde eine einzelne Regel geliefert hat.
 *
 * <p><b>Wozu, wenn die Befunde ohnehin einzeln im Bericht stehen:</b> Auf 1.810 Zeilen
 * meldete V09 (Zukunftsmonat) 302 Warnungen und V08 (Ausreisser) neun. Die neun waren die
 * gefaehrlichen - sie trugen die gesamte Abweichung -, gingen aber zwischen den 302
 * unter, weil der Datenqualitaets-Tab nach Zeilennummer sortiert ist und beide Regelarten
 * durchmischt. Eine Zeile je Regel macht das Verhaeltnis auf einen Blick sichtbar.
 *
 * <p>Auch Regeln mit {@code anzahl == 0} gehoeren in die Uebersicht: Nur so ist "keine
 * Dubletten gefunden" von "nicht auf Dubletten geprueft" zu unterscheiden.
 *
 * @param id          die Kennung der Regel, z.B. "V08"
 * @param bezeichnung was sie prueft
 * @param anzahl      wie viele Befunde sie in diesem Lauf geliefert hat
 */
public record Regelzaehlung(String id, String bezeichnung, long anzahl) {
}
