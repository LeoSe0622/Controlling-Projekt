package de.leo.controlling.rechnung;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Das Ergebnis der Deckungsbeitragsrechnung fuer EINE Seite (Plan oder Ist)
 * einer Produkt-Monats-Kombination.
 *
 * <p>Alle Geldbetraege sind bereits auf zwei Stellen gerundet. Das ist Absicht: Im Excel
 * stehen spaeter zwoelf Monatszeilen und eine Summenzeile, und die Summe muss der Summe
 * der ANGEZEIGTEN Werte entsprechen. Ein Bericht, dessen Spalten sich addieren lassen,
 * ist mehr wert als zwei Nachkommastellen theoretische Genauigkeit.
 *
 * <p>Die drei Kennzahlen unten sind Methoden, keine Komponenten — sie leiten sich
 * vollstaendig aus den gespeicherten Werten ab. Als Konstruktor-Parameter koennte jemand
 * einen Deckungsbeitrag bauen, dessen Marge nicht zu seinem DB I passt.
 */
public record Deckungsbeitrag(
        BigDecimal menge,
        BigDecimal umsatz,
        BigDecimal variableKosten,
        BigDecimal dbEins,
        BigDecimal fixkosten,
        BigDecimal dbZwei
) {

    /** Nachkommastellen fuer Margen: 4 entspricht zwei Stellen in Prozent. */
    private static final int SKALA_MARGE = 4;

    /**
     * Deckungsbeitrag I je Stueck.
     *
     * @return {@code null}, wenn die Menge 0 ist — dann gibt es keinen Stueck-DB.
     *         Bewusst kein 0, das waere eine erfundene Zahl.
     */
    public BigDecimal dbEinsJeStueck() {

        if (menge.signum() == 0) {
            return null;
        }
        return dbEins.divide(menge, 2, RoundingMode.HALF_UP);
    }

    /**
     * DB-I-Marge als Anteil zwischen 0 und 1 (0.5478 = 54,78 %).
     *
     * @return {@code null}, wenn kein Umsatz vorliegt
     */
    public BigDecimal dbEinsMarge() {

        if (umsatz.signum() == 0) {
            return null;
        }
        return dbEins.divide(umsatz, SKALA_MARGE, RoundingMode.HALF_UP);
    }

    /**
     * Break-Even-Menge: wie viele Stueck abgesetzt werden muessen, damit der
     * Deckungsbeitrag I die Fixkosten gerade deckt.
     *
     * <p>Beantwortet die Frage "ab wann traegt sich das Produkt?". Bei Produkt B
     * (Fixkosten 3.000, Stueck-DB 35,00) sind das 86 Stueck im Monat bei 600
     * geplanten - komfortabel. Je naeher der Break-Even an der Planmenge liegt,
     * desto riskanter das Produkt.
     *
     * @return {@code null}, wenn der Stueck-DB fehlt oder nicht positiv ist -
     *         dann gibt es keinen Break-Even, das Produkt traegt sich nie
     */
    public BigDecimal breakEvenMenge() {

        BigDecimal stueckDb = dbEinsJeStueck();

        // Kein Stueck-DB (Menge war 0) -> kein Break-Even.
        if (stueckDb == null) {
            return null;
        }

        // Stueck-DB <= 0: Es gibt keine Menge, ab der sich das Produkt traegt.
        // Mehr zu verkaufen macht es schlimmer. Eine Zahl waere hier eine
        // Falschaussage.
        if (stueckDb.signum() <= 0) {
            return null;
        }

        // CEILING statt HALF_UP: Die Frage lautet "wie viele brauche ich
        // MINDESTENS". Bei 153,2 benoetigten Stueck reichen 153 nicht.
        return fixkosten.divide(stueckDb, 0, RoundingMode.CEILING);
    }

    /** DB-II-Marge, analog zu {@link #dbEinsMarge()}. */
    public BigDecimal dbZweiMarge() {

        // TODO 3: analog
        if (umsatz.signum() == 0) {
            return null;
        }
        return dbZwei.divide(umsatz, SKALA_MARGE, RoundingMode.HALF_UP);
    }
}
