package de.leo.controlling.rechnung;

import java.math.BigDecimal;

/**
 * Das Jahresergebnis eines Produkts: Plan und Ist, aufsummiert ueber alle Monate,
 * die es in den verwertbaren Daten gab.
 *
 * <p><b>Warum {@code monate} eine eigene Komponente ist</b> — und keine Nebensache:
 * Nach der Validierung fehlen einzelnen Produkten Monate — je nachdem, wie viele ihrer
 * Zeilen als FEHLER aussortiert wurden. Andere Produkte sind vollstaendig.
 *
 * <p>Ein Jahres-DB-II ohne diese Zahl ist irrefuehrend: Ein Produkt mit neun Monaten sieht
 * neben einem mit zwoelf systematisch schlechter aus — nicht weil es schlechter laeuft,
 * sondern weil Monate fehlen. Wer die Zahlen nebeneinander legt und die Fussnote nicht hat,
 * zieht einen falschen Schluss. Deshalb gehoert die Monatsabdeckung als eigene Spalte in
 * den Bericht, gleichberechtigt neben die Euro-Betraege.
 */
public record Produktergebnis(
        String produkt,
        Deckungsbeitrag plan,
        Deckungsbeitrag ist,
        int monate
) {

    /** Die absolute DB-II-Abweichung: Ist minus Plan. Positiv = besser als geplant. */
    public BigDecimal dbZweiAbweichung() {

        return ist.dbZwei().subtract(plan.dbZwei());

    }

    /** Ob alle erwarteten Monate vorliegen. */
    public boolean vollstaendig(int erwarteteMonate) {
        return monate == erwarteteMonate;
    }
}
