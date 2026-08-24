package de.leo.controlling.pruefung;

import de.leo.controlling.model.Rohzeile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integrationstest: hier laufen alle acht Regeln zusammen.
 *
 * <p>Die Regel-Tests prueften jede Regel fuer sich. Dieser Test prueft, was passiert,
 * wenn sie gemeinsam auf dieselben Daten losgelassen werden — vor allem, ob der
 * Schweigegrundsatz haelt und ob die Trennung FEHLER/WARNUNG richtig wirkt.
 */
class ValidatorTest {

    @Test
    void leereDateiHatVolleQualitaet() {
        // gepruefteZeilen == 0 waere eine Division durch null. 1.0 ist die
        // sinnvollere Antwort als ein Absturz: Es gibt nichts Kaputtes.
        Pruefprotokoll leer = validator.pruefe(List.of());

        assertEquals(1.0, leer.qualitaetsquote(), 0.001);
        assertEquals(0, leer.gepruefteZeilen());
        assertTrue(leer.befunde().isEmpty());
    }

    private final Validator validator = new Validator();

    @Test
    void saubereZeilenErgebenKeineBefunde() {
        List<Rohzeile> alle = List.of(
                zeile(2, "2025-02", "Produkt C", "Vertrieb Nord", "2000", "2233.0", "25.0", "26.54"),
                zeile(3, "2025-12", "Produkt D", "Vertrieb Sued", "350", "345.0", "120.0", "121.94")
        );

        Pruefprotokoll protokoll = validator.pruefe(alle);

        assertTrue(protokoll.befunde().isEmpty(), "saubere Zeilen duerfen nichts ausloesen");
        assertEquals(2, protokoll.verwertbareZeilen().size());
        assertEquals(2, protokoll.gepruefteZeilen());
        // double immer mit Toleranz vergleichen - exakte Gleichheit ist bei
        // Fliesskomma nicht garantiert.
        assertEquals(1.0, protokoll.qualitaetsquote(), 0.001);
    }

    @Test
    void fehlerzeileFliegtRaus() {
        List<Rohzeile> alle = List.of(
                zeile(2, "2025-02", "Produkt C", "Vertrieb Nord", "2000", "2233.0", "25.0", "26.54"),
                zeile(22, "2025-04", "Produkt B", "Vertrieb Sued", "600", "", "80.0", "83.03")
        );

        Pruefprotokoll protokoll = validator.pruefe(alle);

        assertEquals(1, protokoll.befunde().size());
        assertEquals("V02", protokoll.befunde().get(0).regelId());
        assertEquals(1, protokoll.anzahlFehler());
        assertEquals(0, protokoll.anzahlWarnungen());

        // Nur die saubere Zeile 2 bleibt uebrig.
        assertEquals(1, protokoll.verwertbareZeilen().size());
        assertEquals(2, protokoll.verwertbareZeilen().get(0).zeilennummer());
        assertEquals(0.5, protokoll.qualitaetsquote(), 0.001);
    }

    @Test
    void warnungszeileBleibtDrin() {
        // Der wichtigste Test dieser Klasse: Zeile 46 ist der Ausreisser
        // (istMenge 17950 bei Plan 350). V08 meldet eine WARNUNG - die Zeile
        // bleibt aber in der Rechnung und verzerrt spaeter sichtbar die Kennzahlen.
        List<Rohzeile> alle = List.of(
                zeile(2, "2025-02", "Produkt C", "Vertrieb Nord", "2000", "2233.0", "25.0", "26.54"),
                zeile(46, "2025-06", "Produkt D", "Vertrieb Nord", "350", "17950.0", "120.0", "111.54")
        );

        Pruefprotokoll protokoll = validator.pruefe(alle);

        assertEquals(1, protokoll.befunde().size());
        assertEquals("V08", protokoll.befunde().get(0).regelId());
        assertEquals(Schweregrad.WARNUNG, protokoll.befunde().get(0).grad());

        assertEquals(0, protokoll.anzahlFehler());
        assertEquals(1, protokoll.anzahlWarnungen());

        // Beide Zeilen bleiben verwertbar - eine Warnung schliesst nicht aus.
        assertEquals(2, protokoll.verwertbareZeilen().size());
        assertEquals(1.0, protokoll.qualitaetsquote(), 0.001,
                "eine Warnung senkt die Datenqualitaetsquote nicht");
    }

    @Test
    void beideDublettenZeilenFliegenRaus() {
        List<Rohzeile> alle = List.of(
                zeile(6, "2025-04", "Produkt A", "Vertrieb Nord", "1000", "957.0", "50.0", "53.83"),
                zeile(10, "2025-05", "Produkt B", "Vertrieb Sued", "600", "551.0", "80.0", "74.93"),
                zeile(21, "2025-04", "Produkt A", "Vertrieb Nord", "1000", "957.0", "50.0", "53.83")
        );

        Pruefprotokoll protokoll = validator.pruefe(alle);

        assertEquals(2, protokoll.anzahlFehler(), "beide Dublettenzeilen werden gemeldet");
        assertTrue(protokoll.befunde().stream().allMatch(b -> b.regelId().equals("V07")));

        // Nur die eindeutige Zeile 10 bleibt uebrig.
        assertEquals(1, protokoll.verwertbareZeilen().size());
        assertEquals(10, protokoll.verwertbareZeilen().get(0).zeilennummer());
    }

    @Test
    void negativerPreisErzeugtNurEinenBefund() {
        // Der Schweigegrundsatz unter Volllast: Zeile 14 (istPreis = -77.05) koennte
        // V05 (negativ), V06 (Kosten 45 > Preis -77.05) UND V08 (196 % Abweichung)
        // ausloesen. Es darf aber nur EIN Befund entstehen - der von V05, der die
        // Ursache benennt. Alles andere waere Rauschen im Bericht.
        List<Rohzeile> alle = List.of(
                zeile(14, "2025-09", "Produkt B", "Vertrieb Sued", "600", "761.0", "80.0", "-77.05")
        );

        Pruefprotokoll protokoll = validator.pruefe(alle);

        assertEquals(1, protokoll.befunde().size(),
                "V06 und V08 muessen bei einem negativen Preis schweigen");
        assertEquals("V05", protokoll.befunde().get(0).regelId());
        assertEquals("istPreis", protokoll.befunde().get(0).feld());
    }

    /** Baut eine Zeile; variable Stueckkosten und Fixkosten sind immer plausibel. */
    private static Rohzeile zeile(int nr, String monat, String produkt, String kostenstelle,
                                  String planMenge, String istMenge, String planPreis, String istPreis) {
        String kosten = switch (produkt) {
            case "Produkt A" -> "30.0";
            case "Produkt B" -> "45.0";
            case "Produkt C" -> "12.0";
            default -> "70.0";
        };
        return new Rohzeile(nr, 9, monat, produkt, kostenstelle,
                planMenge, istMenge, planPreis, istPreis, kosten, "3000");
    }

    /**
     * Die Befunde kommen nach Zeilennummer sortiert heraus - auch ueber beide Regelarten
     * hinweg.
     *
     * <p>Die Dublettenregel ist eine Datensatzregel und laeuft NACH allen Zeilenregeln.
     * Ohne die Sortierung stuende der V02-Befund der Zeile 20 vor den V07-Befunden der
     * Zeilen 5 und 30: Der Bericht faengt in der Mitte wieder von vorn an, und wer wissen
     * will, was mit einer bestimmten Zeile los ist, muss an zwei Stellen suchen.
     */
    @Test
    void befundeKommenNachZeilennummerSortiert() {
        List<Rohzeile> alle = List.of(
                zeile(5, "2025-04", "Produkt A", "Vertrieb Nord", "1000", "957.0", "50.0", "53.83"),
                zeile(20, "2025-05", "Produkt B", "Vertrieb Sued", "600", "", "80.0", "83.03"),
                zeile(30, "2025-04", "Produkt A", "Vertrieb Nord", "1000", "957.0", "50.0", "53.83")
        );

        List<Integer> nummern = validator.pruefe(alle).befunde().stream()
                .map(Befund::zeilennummer)
                .toList();

        assertEquals(List.of(5, 20, 30), nummern);
    }
}
