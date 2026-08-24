package de.leo.controlling.report;

import de.leo.controlling.Testdaten;
import de.leo.controlling.abweichung.Wesentlichkeit;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Pruefprotokoll;
import de.leo.controlling.pruefung.Schweregrad;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests fuer {@link Berichtsmodell#statusVon(int)} — die Statusspalte im Rohdaten-Tab.
 *
 * <p>Die ersten drei Tests laufen gegen die Testdatei. Die uebrigen brauchen eine Zeile
 * mit FEHLER UND WARNUNG gleichzeitig; die gibt es dort nicht, also wird sie von Hand
 * gebaut. Genau dieser Fall entscheidet ueber die Rangfolge.
 */
class StatusVonTest {

    @Test
    void saubereZeileHatKeinenStatus() throws IOException {
        assertNull(Testdaten.modell().statusVon(2),
                "null heisst in Ordnung - im Tab steht dann OK");
    }

    @Test
    void fehlerzeileWirdAlsFehlerGemeldet() throws IOException {
        Berichtsmodell m = Testdaten.modell();

        assertEquals(Schweregrad.FEHLER, m.statusVon(9), "leeres istMenge -> V02");
        assertEquals(Schweregrad.FEHLER, m.statusVon(19), "negativer Preis -> V05");
        assertEquals(Schweregrad.FEHLER, m.statusVon(5), "Dublette -> V07");
    }

    @Test
    void warnzeileWirdAlsWarnungGemeldet() throws IOException {
        assertEquals(Schweregrad.WARNUNG, Testdaten.modell().statusVon(17),
                "der Ausreisser bleibt in der Rechnung, wird aber markiert");
    }

    /**
     * Wer Fehler und Warnung hat, ist ein Fehler — sonst zeigt der Rohdaten-Tab eine
     * gelbe Zeile fuer einen Datensatz, der aus der Rechnung geflogen ist.
     */
    @Test
    void fehlerSchlaegtWarnung() {
        Berichtsmodell m = modellMit(
                befund(7, Schweregrad.WARNUNG),
                befund(7, Schweregrad.FEHLER));

        assertEquals(Schweregrad.FEHLER, m.statusVon(7));
    }

    /**
     * Dasselbe mit vertauschter Reihenfolge. Eine Implementierung, die einfach den
     * ERSTEN Befund nimmt, waere bei einer Reihenfolge richtig und bei der anderen
     * falsch — nur beide Tests zusammen schliessen das aus.
     */
    @Test
    void reihenfolgeDerBefundeIstEgal() {
        Berichtsmodell m = modellMit(
                befund(7, Schweregrad.FEHLER),
                befund(7, Schweregrad.WARNUNG));

        assertEquals(Schweregrad.FEHLER, m.statusVon(7));
    }

    @Test
    void befundeAndererZeilenZaehlenNicht() {
        Berichtsmodell m = modellMit(befund(99, Schweregrad.FEHLER));

        assertNull(m.statusVon(7), "der Fehler gehoert zu Zeile 99, nicht zu Zeile 7");
    }

    private static Befund befund(int zeile, Schweregrad grad) {
        return new Befund(zeile, "istPreis", "V05", grad, "-1", "Testbefund");
    }

    /**
     * Baut ein Berichtsmodell, das nur das Pruefprotokoll enthaelt.
     *
     * <p>Die uebrigen Komponenten bleiben leer: statusVon() greift ausschliesslich auf
     * protokoll() zu. Ein vollstaendiges Modell zu bauen wuerde den Test aufblaehen,
     * ohne ihn aussagekraeftiger zu machen.
     */
    private static Berichtsmodell modellMit(Befund... befunde) {
        return new Berichtsmodell(
                "test.csv", Testdaten.STICHTAG, 1,
                List.of(),
                new Pruefprotokoll(List.of(befunde), List.of(), 1),
                List.of(), Map.of(),
                null, null,
                Warnzeileneinfluss.leer(),
                List.of(), List.of(),
                Wesentlichkeit.standard());
    }
}
