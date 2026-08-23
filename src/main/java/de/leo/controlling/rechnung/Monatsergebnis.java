package de.leo.controlling.rechnung;

import java.time.YearMonth;

/**
 * Ergebnis einer Produkt-Monats-Kombination — die Grundlage fuer die Zeitreihe.
 *
 * <p>{@link Produktergebnis} fasst ueber alle Monate zusammen und beantwortet
 * "wie lief das Jahr". Hier bleibt die Monatsebene erhalten und beantwortet
 * "wann lief es wie" — ob ein Rueckgang gleichmaessig war oder ein einzelner
 * Monat ausbricht, sieht man nur so.
 */
public record Monatsergebnis(
        String produkt,
        YearMonth monat,
        Deckungsbeitrag plan,
        Deckungsbeitrag ist
) {
}
