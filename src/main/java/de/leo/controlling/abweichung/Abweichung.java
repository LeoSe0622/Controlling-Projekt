package de.leo.controlling.abweichung;

import java.math.BigDecimal;

/**
 * Die Zerlegung einer DB-Abweichung in ihre Ursachen.
 *
 * <p><b>Vorzeichenkonvention: "+" heisst ergebnisverbessernd.</b> Bei allen vier Werten.
 * Deshalb steht bei der Fixkostenabweichung ein Minus vor der Klammer — hoehere Ist-
 * Fixkosten verschlechtern das Ergebnis und ergeben eine NEGATIVE Abweichung.
 *
 * <p>Das ist die haeufigste Fehlerquelle solcher Rechnungen. Wer hier ein Vorzeichen
 * dreht, produziert Zahlen, die plausibel aussehen und das Gegenteil aussagen.
 *
 * @param preisabweichung     (p_ist - p_plan) x m_plan
 * @param mengenabweichung    (m_ist - m_plan) x (p_plan - k)
 * @param mischabweichung     (p_ist - p_plan) x (m_ist - m_plan)
 * @param fixkostenabweichung -(F_ist - F_plan); in dieser CSV immer 0, weil die
 *                            Fixkosten je Zeile fuer Plan und Ist identisch sind.
 *                            Die Formel bleibt trotzdem drin, damit der Bericht
 *                            auch mit anderen Daten stimmt.
 */
public record Abweichung(
        BigDecimal preisabweichung,
        BigDecimal mengenabweichung,
        BigDecimal mischabweichung,
        BigDecimal fixkostenabweichung
) {

    /**
     * Die Gesamtabweichung — muss der Differenz der DB II entsprechen.
     * Das ist die Abstimmbruecke.
     */
    public BigDecimal gesamt() {

        return preisabweichung.add(mengenabweichung).add(mischabweichung).add(fixkostenabweichung);
    }
}
