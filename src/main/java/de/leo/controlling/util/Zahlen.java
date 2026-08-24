package de.leo.controlling.util;

import java.math.BigDecimal;

/**
 * Wandelt Rohwerte aus der CSV in Zahlen um.
 *
 * <p>Der entscheidende Punkt: Diese Klasse <b>wirft nichts</b>. Eine Regel, die pruefen
 * will, ob ein Feld eine Zahl ist, moechte fragen koennen — nicht einen try/catch um jede
 * Pruefung bauen. Deshalb ist die Antwort auf "das ist keine Zahl" ein {@code null},
 * kein Kontrollfluss ueber Exceptions.
 *
 * <p>Warum {@code BigDecimal} und nicht {@code double}: Geld muss auf den Cent stimmen.
 * {@code 0.1 + 0.2} ergibt in {@code double} nicht {@code 0.3}, und solche Fehler
 * summieren sich ueber viele Zeilen zu einer Abstimmbruecke, die nicht aufgeht.
 */
public final class Zahlen {

    /** Utility-Klasse: es gibt nichts zu instanziieren. */
    private Zahlen() {
    }

    /**
     * @param text der Rohwert aus der CSV, z.B. "2233.0"
     * @return die Zahl — oder {@code null}, wenn text leer, {@code null} oder keine
     *         gueltige Zahl ist
     */
    public static BigDecimal parse(String text) {

        if ( text == null || text.isBlank() ) {
            return null;
        }
        try {
           return new BigDecimal(text.trim());
         } catch (NumberFormatException e) {
         return null;
         }
        
    }
}
