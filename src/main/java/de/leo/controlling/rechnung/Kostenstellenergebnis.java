package de.leo.controlling.rechnung;

/**
 * Ergebnis je Kostenstelle — die zweite Sicht auf dieselben Daten.
 *
 * <p><b>Wichtige Einschraenkung, die in den Bericht gehoert:</b> Der DB II enthaelt die
 * produktfixen Kosten. Die gehoeren fachlich zum PRODUKT, nicht zur Vertriebsregion —
 * sie werden hier derjenigen Kostenstelle zugerechnet, die in dem Monat zufaellig auf
 * der Zeile stand. In den vorliegenden Daten wechselt die Kostenstelle je Produkt von
 * Monat zu Monat, die Zuordnung ist also weitgehend willkuerlich.
 *
 * <p>Aussagekraeftig ist deshalb vor allem der <b>DB I</b>: Umsatz minus variable Kosten
 * laesst sich einer Region sauber zurechnen, produktfixe Kosten nicht. Der DB II steht
 * der Vollstaendigkeit halber daneben und ist mit Vorsicht zu lesen.
 *
 * @param zeilen wie viele Datenzeilen in diese Kostenstelle geflossen sind
 */
public record Kostenstellenergebnis(
        String kostenstelle,
        Deckungsbeitrag plan,
        Deckungsbeitrag ist,
        int zeilen
) {
}
