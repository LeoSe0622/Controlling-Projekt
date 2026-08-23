package de.leo.controlling.report;

import de.leo.controlling.abweichung.Wesentlichkeit;
import de.leo.controlling.io.CsvEinleser;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Pruefprotokoll;
import de.leo.controlling.pruefung.Schweregrad;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests fuer {@link Berichtsmodell#statusVon(int)} - die Statusspalte im Rohdaten-Tab.
 *
 * <p>Die ersten drei Tests laufen gegen die echten Daten. Der vierte braucht eine Zeile
 * mit FEHLER UND WARNUNG gleichzeitig - die gibt es in der echten CSV nicht, also wird
 * sie von Hand gebaut. Genau dieser Fall entscheidet ueber die Rangfolge, und genau
 * deshalb muss er geprueft werden.
 */
class StatusVonTest {

    private static final LocalDateTime STICHTAG = LocalDateTime.of(2025, 12, 31, 23, 59);

    private static Berichtsmodell echtesModell() throws IOException {
        return new BerichtsmodellBauer().baue(
                "controlling_rohdaten.csv",
                new CsvEinleser().lies(Path.of("controlling_rohdaten.csv")),
                STICHTAG);
    }

    @Test
    void saubereZeileHatKeinenStatus() throws IOException {
        // Zeile 2 ist unauffaellig. null heisst "in Ordnung" - im Tab steht dann "OK".
        assertNull(echtesModell().statusVon(2));
    }

    @Test
    void fehlerzeileWirdAlsFehlerGemeldet() throws IOException {
        // Zeile 22: leeres istMenge -> V02, FEHLER
        assertEquals(Schweregrad.FEHLER, echtesModell().statusVon(22));

        // Zeile 14: negativer Preis -> V05, FEHLER
        assertEquals(Schweregrad.FEHLER, echtesModell().statusVon(14));
    }

    @Test
    void warnzeileWirdAlsWarnungGemeldet() throws IOException {
        // Zeile 46: der Ausreisser -> V08, WARNUNG.
        // Die Zeile bleibt in der Rechnung, wird aber markiert.
        assertEquals(Schweregrad.WARNUNG, echtesModell().statusVon(46));
    }

    @Test
    void fehlerSchlaegtWarnung() {
        // Der Fall, den die echten Daten nicht hergeben: eine Zeile mit BEIDEM.
        // Wer Fehler und Warnung hat, ist ein Fehler - sonst zeigt der Rohdaten-Tab
        // eine gelbe Zeile fuer einen Datensatz, der aus der Rechnung geflogen ist.
        Berichtsmodell m = modellMit(
                befund(7, Schweregrad.WARNUNG, "V08"),
                befund(7, Schweregrad.FEHLER, "V05"));

        assertEquals(Schweregrad.FEHLER, m.statusVon(7));
    }

    @Test
    void reihenfolgeDerBefundeIstEgal() {
        // Dasselbe nochmal mit vertauschter Reihenfolge. Eine Implementierung, die
        // einfach den ERSTEN Befund nimmt, wuerde hier gruen und oben rot sein -
        // oder umgekehrt. Beide Faelle muessen dasselbe ergeben.
        Berichtsmodell m = modellMit(
                befund(7, Schweregrad.FEHLER, "V05"),
                befund(7, Schweregrad.WARNUNG, "V08"));

        assertEquals(Schweregrad.FEHLER, m.statusVon(7));
    }

    @Test
    void befundeAndererZeilenZaehlenNicht() {
        Berichtsmodell m = modellMit(befund(99, Schweregrad.FEHLER, "V05"));

        assertNull(m.statusVon(7), "der Fehler gehoert zu Zeile 99, nicht zu Zeile 7");
    }

    private static Befund befund(int zeile, Schweregrad grad, String regel) {
        return new Befund(zeile, "istPreis", regel, grad, "-1", "Testbefund");
    }

    /**
     * Baut ein Berichtsmodell, das nur das Pruefprotokoll enthaelt.
     *
     * <p>Die uebrigen Komponenten bleiben leer bzw. {@code null}: statusVon() greift
     * ausschliesslich auf protokoll() zu. Ein vollstaendiges Modell zu bauen wuerde
     * den Test aufblaehen, ohne ihn aussagekraeftiger zu machen.
     */
    private static Berichtsmodell modellMit(Befund... befunde) {
        return new Berichtsmodell(
                "test.csv", STICHTAG, 1,
                List.of(),
                new Pruefprotokoll(List.of(befunde), List.of(), 1),
                List.of(), Map.of(),
                null, null,
                List.of(), List.of(),
                Wesentlichkeit.standard());
    }
}
