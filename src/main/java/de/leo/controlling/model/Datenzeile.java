package de.leo.controlling.model;

import de.leo.controlling.util.Zahlen;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * Eine geprüfte Zeile mit echten Typen — das Gegenstück zu {@link Rohzeile}.
 *
 * <p>Wo die Rohzeile alles als {@code String} haelt, stehen hier {@code BigDecimal} und
 * {@link YearMonth}. Damit ist sie rechenbar: {@code menge.multiply(preis)} geht,
 * {@code "2233.0" + "26.54"} ging nicht.
 *
 * <p><b>Die spaltenAnzahl faellt weg.</b> Sie war nur fuer Regel V01 da. Jede Stufe traegt
 * nur das weiter, was die naechste braucht — sonst schleppt man Ballast durch das ganze
 * Programm.
 *
 * <p><b>Umgekehrte Fehlerhaltung.</b> Eine Datenzeile entsteht ausschliesslich aus
 * verwertbaren Zeilen; die haben V02, V03 und V05 passiert. Ein nicht parsebarer Wert kann
 * hier also nur bedeuten, dass jemand die Validierung uebersprungen hat — ein Programmier-
 * fehler, kein Datenfehler. Deshalb wird hier geworfen statt gemeldet:
 *
 * <ul>
 *   <li>CsvEinleser und Regeln: nie abstuerzen, immer melden</li>
 *   <li>Datenzeile: sofort abstuerzen, laut und mit klarer Meldung</li>
 * </ul>
 *
 * Ein stiller Fallback (etwa "dann eben 0") waere hier das Schlimmste — er wuerde falsche
 * Zahlen produzieren, die niemand mehr hinterfragt.
 */
public record Datenzeile(
        int zeilennummer,
        YearMonth monat,
        String produkt,
        String kostenstelle,
        BigDecimal planMenge,
        BigDecimal istMenge,
        BigDecimal planPreis,
        BigDecimal istPreis,
        BigDecimal variableStueckkosten,
        BigDecimal fixkostenProdukt
) {

    /**
     * Wandelt eine geprüfte Rohzeile um.
     *
     * @throws IllegalArgumentException wenn ein Wert nicht interpretierbar ist —
     *                                  das bedeutet, die Validierung wurde uebersprungen
     */
    public static Datenzeile aus(Rohzeile roh) {

        return new Datenzeile(
                roh.zeilennummer(),
                monat(roh),
                roh.produkt().trim(),
                roh.kostenstelle().trim(),
                zahl(roh, "planMenge", roh.planMenge()),
                zahl(roh, "istMenge", roh.istMenge()),
                zahl(roh, "planPreis", roh.planPreis()),
                zahl(roh, "istPreis", roh.istPreis()),
                zahl(roh, "variableStueckkosten", roh.variableStueckkosten()),
                zahl(roh, "fixkostenProdukt", roh.fixkostenProdukt())
        );
    }

    /** Parst ein Zahlenfeld — oder wirft, weil die Validierung das haette abfangen muessen. */
    private static BigDecimal zahl(Rohzeile roh, String feldname, String wert) {

        if (Zahlen.parse(wert) != null) {
            return Zahlen.parse(wert);
        }
        throw new IllegalArgumentException(
                "Zeile " + roh.zeilennummer() + ", Feld " + feldname
                        + ": '" + wert + "' ist keine Zahl. "
                        + "Wurde die Validierung uebersprungen?");
    }

    /** Parst den Monat — oder wirft, siehe oben. */
    private static YearMonth monat(Rohzeile roh) {

        try{
            return YearMonth.parse(roh.monat().trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Zeile " + roh.zeilennummer() + ", Feld monat: '" + roh.monat() + "' ist kein gueltiger Monat. "
                            + "Wurde die Validierung uebersprungen?", e);
        }
    }
}
