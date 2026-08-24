package de.leo.controlling.model;

/**
 * Eine Zeile der Rohdaten-CSV — unverändert so, wie sie in der Datei stand.
 *
 * <p>Bewusst sind ALLE Datenfelder {@code String}: In dieser Stufe wird nichts geparst
 * und nichts geprüft. Enthaelt eine Zeile ein leeres {@code ist_menge}, wuerden wir hier
 * schon ein {@code BigDecimal} daraus machen, würde das Programm an dieser Zeile
 * abstürzen — und wir könnten hinterher nicht mehr berichten, WO und WAS kaputt war.
 * Genau das ist aber der Zweck des Projekts.
 *
 * <p>Das Parsen in echte Zahlen passiert später, nach der Validierung, in einer eigenen
 * Klasse {@code Datenzeile}.
 *
 * @param zeilennummer   1-basiert wie im Editor: Kopfzeile = 1, erste Datenzeile = 2.
 *                      Kein Wert aus der CSV, sondern Herkunftsinformation für
 *                      Fehlermeldungen ("Zeile 22, Feld istMenge, war leer" — die
 *                      Nummer stammt aus der jeweils eingelesenen Datei).
 * @param spaltenAnzahl wie viele Spalten die Zeile tatsächlich hatte (erwartet: 9).
 *                      Der Einleser füllt fehlende Spalten mit "" auf, damit nichts
 *                      abstürzt — dadurch wäre sonst nicht mehr unterscheidbar, ob ein
 *                      Feld leer war oder ganz gefehlt hat. Regel V01 braucht das.
 */
public record Rohzeile(
        int zeilennummer,
        int spaltenAnzahl,
        String monat,
        String produkt,
        String kostenstelle,
        String planMenge,
        String istMenge,
        String planPreis,
        String istPreis,
        String variableStueckkosten,
        String fixkostenProdukt
) {
}
