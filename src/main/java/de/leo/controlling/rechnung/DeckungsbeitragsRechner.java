package de.leo.controlling.rechnung;

import de.leo.controlling.model.Datenzeile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Rechnet Deckungsbeitraege aus. Reine Funktionen: kein Zustand, keine I/O,
 * dieselbe Eingabe ergibt immer dieselbe Ausgabe.
 *
 * <p>Genau deshalb ist diese Klasse trivial testbar — man braucht keine Datei, keine
 * Datenbank und keinen Aufbau, sondern gibt Zahlen hinein und vergleicht Zahlen.
 *
 * <p>Plan und Ist unterscheiden sich nur darin, WELCHE Menge und WELCHER Preis
 * verwendet werden. Die variablen Stueckkosten gelten laut CSV fuer beide Seiten —
 * deshalb gibt es hier keine Kostenabweichung, und deshalb rechnen beide Methoden
 * ueber dieselbe private Methode.
 */
public final class DeckungsbeitragsRechner {

    /** Nachkommastellen fuer Geldbetraege. */
    private static final int SKALA_GELD = 2;

    /**
     * Summiert mehrere Deckungsbeitraege - etwa die zwoelf Monate eines Produkts.
     *
     * <p>Alle sechs Werte werden addiert, auch die Fixkosten: Sie sind laut CSV ein
     * MONATSwert, zwoelf Monate ergeben also die Jahresfixkosten. Weil die Dublettenregel
     * dafuer gesorgt hat, dass es je (Produkt, Monat) nur eine Zeile gibt, kann dabei
     * nichts doppelt gezaehlt werden.
     *
     * @return bei leerer Liste ein Deckungsbeitrag aus lauter Nullen
     */
    public Deckungsbeitrag summe(List<Deckungsbeitrag> teile) {

        BigDecimal menge = BigDecimal.ZERO.setScale(SKALA_GELD);
        BigDecimal umsatz = BigDecimal.ZERO.setScale(SKALA_GELD);
        BigDecimal variableKosten = BigDecimal.ZERO.setScale(SKALA_GELD);
        BigDecimal dbEins = BigDecimal.ZERO.setScale(SKALA_GELD);
        BigDecimal fixkosten = BigDecimal.ZERO.setScale(SKALA_GELD);
        BigDecimal dbZwei = BigDecimal.ZERO.setScale(SKALA_GELD);

        for(Deckungsbeitrag t : teile) {
            menge = menge.add(t.menge());
            umsatz = umsatz.add(t.umsatz());
            variableKosten = variableKosten.add(t.variableKosten());
            dbEins = dbEins.add(t.dbEins());
            fixkosten = fixkosten.add(t.fixkosten());
            dbZwei = dbZwei.add(t.dbZwei());

        }
        return new Deckungsbeitrag(menge, umsatz, variableKosten, dbEins, fixkosten, dbZwei);
    }

    /** Die Plan-Seite einer Zeile. */
    public Deckungsbeitrag plan(Datenzeile z) {

        return rechne(z.planMenge(), z.planPreis(),z.variableStueckkosten(), z.fixkostenProdukt());
    }

    /** Die Ist-Seite einer Zeile. */
    public Deckungsbeitrag ist(Datenzeile z) {

        return rechne(z.istMenge(), z.istPreis(), z.variableStueckkosten(), z.fixkostenProdukt());
    }

    /**
     * Die eigentliche Rechnung — einmal geschrieben, von beiden Seiten benutzt.
     *
     * <pre>
     *   Umsatz          = menge x preis
     *   Variable Kosten = menge x k
     *   DB I            = Umsatz - Variable Kosten
     *   DB II           = DB I - Fixkosten
     * </pre>
     *
     * <p>Gerundet wird, BEVOR weitergerechnet wird: {@code dbEins} entsteht aus dem
     * bereits gerundeten Umsatz und den gerundeten variablen Kosten. Andernfalls
     * waere der angezeigte DB I gelegentlich einen Cent von "Umsatz minus variable
     * Kosten" der angezeigten Zeile entfernt - und jemand sucht eine Stunde nach der
     * Differenz. Ein Bericht, dessen Spalten sich addieren lassen, ist mehr wert als
     * zwei Nachkommastellen theoretische Genauigkeit.
     */
    private static Deckungsbeitrag rechne(BigDecimal menge, BigDecimal preis,
                                          BigDecimal k, BigDecimal fixkosten) {

        BigDecimal umsatz = menge.multiply(preis).setScale(SKALA_GELD, RoundingMode.HALF_UP);
        BigDecimal variableKosten = menge.multiply(k).setScale(SKALA_GELD, RoundingMode.HALF_UP);
        BigDecimal dbEins = umsatz.subtract(variableKosten);
        BigDecimal dbZwei = dbEins.subtract(fixkosten).setScale(SKALA_GELD, RoundingMode.HALF_UP);

        return new Deckungsbeitrag(menge, umsatz, variableKosten, dbEins, fixkosten, dbZwei);
    }
}
