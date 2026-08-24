package de.leo.controlling.pruefung.regeln;

import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Schweregrad;
import de.leo.controlling.pruefung.Zeilenregel;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * V09 - die Zeile traegt Ist-Werte fuer einen Monat, der noch nicht vorbei ist.
 *
 * <p>WARNUNG, nicht FEHLER: Die Zahlen sind rechenbar, nur kann es sie noch nicht geben.
 * Ein Ist-Umsatz fuer den Dezember, waehrend man im August berichtet, ist entweder ein
 * vertauschtes Jahr, eine Planzahl in der falschen Spalte oder ein Testdatensatz, der
 * versehentlich in den Produktivlauf geraten ist. Keinen dieser Faelle kann das Programm
 * unterscheiden - also meldet es und rechnet weiter.
 *
 * <p>Der Berichtsmonat selbst wird NICHT beanstandet, obwohl er beim Berichten meist noch
 * laeuft. Sonst schlaegt die Regel bei jedem Monatsbericht auf dem aktuellen Monat an und
 * gewoehnt den Leser daran, sie zu ueberlesen.
 *
 * <p>Schweigegrundsatz: Bei leerem oder unlesbarem Monat sagt diese Regel nichts. Dafuer
 * sind V02 und V04 zustaendig, und ein kaputtes Feld soll genau einen Befund erzeugen.
 */
public class ZukunftsmonatRegel implements Zeilenregel {

    private static final String ID = "V09";

    private final YearMonth berichtsmonat;

    /**
     * @param berichtsmonat der Monat, in dem berichtet wird - alles danach gilt als Zukunft.
     *                      Wird uebergeben statt hier geholt, damit Tests reproduzierbar
     *                      bleiben: Eine Regel, die sich das heutige Datum selbst besorgt,
     *                      liefert im naechsten Monat ein anderes Ergebnis.
     */
    public ZukunftsmonatRegel(YearMonth berichtsmonat) {
        this.berichtsmonat = berichtsmonat;
    }

    @Override
    public List<Befund> pruefe(Rohzeile zeile) {
        List<Befund> befunde = new ArrayList<>();

        if (zeile.monat() == null || zeile.monat().isBlank()) {
            return befunde;
        }

        YearMonth monat;
        try {
            monat = YearMonth.parse(zeile.monat().trim());
        } catch (DateTimeParseException e) {
            return befunde;
        }

        if (monat.isAfter(berichtsmonat)) {
            befunde.add(new Befund(
                    zeile.zeilennummer(),
                    "monat",
                    ID,
                    Schweregrad.WARNUNG,
                    zeile.monat(),
                    "Monat " + zeile.monat() + " liegt nach dem Berichtsmonat "
                            + berichtsmonat + " - fuer diesen Monat kann es noch keine "
                            + "Ist-Werte geben"
            ));
        }

        return befunde;
    }
}
