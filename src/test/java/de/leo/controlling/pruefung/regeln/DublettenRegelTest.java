package de.leo.controlling.pruefung.regeln;

import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Schweregrad;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DublettenRegelTest {

    private final DublettenRegel regel = new DublettenRegel();

    @Test
    void meldetNichts_beiEindeutigenZeilen() {
        List<Rohzeile> alle = List.of(
                zeile(2, "2025-02", "Produkt C", "Vertrieb Nord"),
                zeile(3, "2025-12", "Produkt D", "Vertrieb Sued"),
                zeile(4, "2025-03", "Produkt D", "Vertrieb Sued")
        );

        List<Befund> befunde = regel.pruefe(alle);

        assertTrue(befunde.isEmpty(), "drei verschiedene Kombinationen - keine Dublette");
    }

    @Test
    void meldetFehler_fuerBeideZeilenEinerDublette() {
        // Das sind CSV-Zeile 6 und 21 aus deinen echten Daten.
        List<Rohzeile> alle = List.of(
                zeile(6, "2025-04", "Produkt A", "Vertrieb Nord"),
                zeile(10, "2025-05", "Produkt B", "Vertrieb Sued"),
                zeile(21, "2025-04", "Produkt A", "Vertrieb Nord")
        );

        List<Befund> befunde = regel.pruefe(alle);

        // ZWEI Befunde, nicht einer: Das Programm kann nicht wissen, welche Zeile
        // die richtige ist - also fliegen beide raus und beide werden gemeldet.
        assertEquals(2, befunde.size());

        for (Befund b : befunde) {
            assertEquals("V07", b.regelId());
            assertEquals(Schweregrad.FEHLER, b.grad());
        }

        // Beide beteiligten Zeilen sind dabei - in der Reihenfolge der Datei.
        assertEquals(6, befunde.get(0).zeilennummer());
        assertEquals(21, befunde.get(1).zeilennummer());

        // Zeile 10 ist eindeutig und darf keinen Befund bekommen.
        assertTrue(befunde.stream().noneMatch(b -> b.zeilennummer() == 10),
                "die eindeutige Zeile 10 darf nicht gemeldet werden");
    }

    @Test
    void meldetNichts_beiGleichemProduktInAnderemMonat() {
        // Produkt A kommt zwoelfmal vor - aber in verschiedenen Monaten.
        // Das ist keine Dublette, sondern der Normalfall.
        List<Rohzeile> alle = List.of(
                zeile(6, "2025-04", "Produkt A", "Vertrieb Nord"),
                zeile(7, "2025-05", "Produkt A", "Vertrieb Nord")
        );

        List<Befund> befunde = regel.pruefe(alle);

        assertTrue(befunde.isEmpty(), "verschiedene Monate - keine Dublette");
    }

    @Test
    void meldetNichts_beiGleichemProduktAndererKostenstelle() {
        // Gleicher Monat, gleiches Produkt - aber andere Kostenstelle.
        // Dieser Test haelt fest, dass die kostenstelle Teil des Schluessels ist.
        List<Rohzeile> alle = List.of(
                zeile(6, "2025-04", "Produkt A", "Vertrieb Nord"),
                zeile(7, "2025-04", "Produkt A", "Vertrieb Sued")
        );

        List<Befund> befunde = regel.pruefe(alle);

        assertTrue(befunde.isEmpty(), "verschiedene Kostenstellen - keine Dublette");
    }

    @Test
    void meldetAlleZeilen_beiDreifacherDublette() {
        // Drei identische Kombinationen -> drei Befunde, nicht zwei.
        List<Rohzeile> alle = List.of(
                zeile(6, "2025-04", "Produkt A", "Vertrieb Nord"),
                zeile(21, "2025-04", "Produkt A", "Vertrieb Nord"),
                zeile(35, "2025-04", "Produkt A", "Vertrieb Nord")
        );

        List<Befund> befunde = regel.pruefe(alle);

        assertEquals(3, befunde.size(), "jede beteiligte Zeile wird gemeldet");
    }

    /** Baut eine Zeile, bei der nur die drei Schluesselfelder interessieren. */
    private static Rohzeile zeile(int nr, String monat, String produkt, String kostenstelle) {
        return new Rohzeile(nr, 9, monat, produkt, kostenstelle,
                "1000", "957.0", "50.0", "53.83", "30.0", "5000");
    }
}
