package de.leo.controlling.report;

import de.leo.controlling.rechnung.Deckungsbeitrag;
import de.leo.controlling.rechnung.DeckungsbeitragsRechner;

import java.math.BigDecimal;
import java.util.List;

/**
 * Wie viel vom Ergebnis auf Zeilen beruht, die die Pruefung beanstandet hat.
 *
 * <p>Zeilen mit WARNUNG bleiben in der Rechnung - das ist Absicht, ein Ausreisser soll
 * sichtbar bleiben statt stillschweigend geglaettet zu werden. Damit daraus kein Eigentor
 * wird, muss der Bericht sagen koennen, wie schwer diese Zeilen wiegen.
 *
 * <p><b>Der Anlass war ein echter Fall.</b> Neun von 1756 verwertbaren Zeilen trugen eine
 * Ist-Menge um das Zwanzig- bis Fuenfundvierzigfache des Plans - erkennbar Tippfehler, eine
 * Ziffer zu viel. Diese neun Zeilen machten 102 % der Gesamtabweichung aus: Der Bericht
 * meldete 1,4 Mio EUR ueber Plan, ohne sie lag das Ergebnis 32.634 EUR UNTER Plan. Jede
 * Zahl war richtig gerechnet und die Kernaussage trotzdem falsch herum.
 *
 * <p>Deshalb wirft der Bericht die Zeilen nicht weg, sondern stellt beide Zahlen
 * nebeneinander. Der Leser entscheidet, welche er glaubt.
 *
 * @param zeilen wie viele verwertbare Zeilen mindestens eine Warnung tragen
 * @param plan   Plan-Seite, nur ueber diese Zeilen gerechnet
 * @param ist    Ist-Seite, nur ueber diese Zeilen gerechnet
 */
public record Warnzeileneinfluss(int zeilen, Deckungsbeitrag plan, Deckungsbeitrag ist) {

    /** Die DB-II-Abweichung, die allein aus diesen Zeilen stammt. */
    public BigDecimal abweichung() {
        return ist.dbZwei().subtract(plan.dbZwei());
    }

    /** Der Fall ohne beanstandete Zeilen: keine Zeilen, alle Betraege 0,00. */
    public static Warnzeileneinfluss leer() {
        Deckungsbeitrag nichts = new DeckungsbeitragsRechner().summe(List.of());
        return new Warnzeileneinfluss(0, nichts, nichts);
    }
}
