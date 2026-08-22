package de.leo.controlling.abweichung;

import de.leo.controlling.io.CsvEinleser;
import de.leo.controlling.model.Datenzeile;
import de.leo.controlling.pruefung.Pruefprotokoll;
import de.leo.controlling.pruefung.Validator;
import de.leo.controlling.rechnung.ProduktRechner;
import de.leo.controlling.rechnung.Produktergebnis;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Integrationstest ueber die ECHTEN Daten: Geht die Abstimmbruecke auch ueber
 * alle 43 verwertbaren Zeilen und vier Produkte auf?
 *
 * <p>Die Einzelzerlegung ist mathematisch exakt. Aber beide Rechenwege runden an
 * unterschiedlichen Stellen auf zwei Nachkommastellen: die Abweichungseffekte je Zeile,
 * die Deckungsbeitraege je Umsatz- und Kostenposten. Ob sich daraus ueber 43 Zeilen
 * Cent-Differenzen ansammeln, laesst sich nicht am Schreibtisch beantworten - nur
 * durch Ausfuehren.
 *
 * <p>Zeigt der Test Differenzen, ist das kein Bug: Dann weist ein Bericht eine
 * Rundungsdifferenz als eigene Zeile aus, statt sie zu verstecken.
 */
class AbstimmbrueckeTest {

    @Test
    void brueckeGehtUeberAlleProdukteAuf() throws IOException {
        List<Datenzeile> daten = echteDaten();
        assertFalse(daten.isEmpty(), "Testdaten wurden nicht gefunden");

        List<Produktergebnis> ergebnisse = new ProduktRechner().jeProdukt(daten);
        Map<String, Abweichung> abweichungen = new AbweichungsRechner().jeProdukt(daten);

        for (Produktergebnis e : ergebnisse) {
            Abweichung a = abweichungen.get(e.produkt());

            BigDecimal ausZerlegung = a.gesamt();
            BigDecimal ausDeckungsbeitrag = e.ist().dbZwei().subtract(e.plan().dbZwei());
            BigDecimal differenz = ausZerlegung.subtract(ausDeckungsbeitrag);

            assertEquals(0, differenz.signum(),
                    e.produkt() + ": Zerlegung " + ausZerlegung
                            + " vs. Deckungsbeitraege " + ausDeckungsbeitrag
                            + " -> Differenz " + differenz);
        }
    }

    @Test
    void brueckeGehtAufGesamtebeneAuf() throws IOException {
        List<Datenzeile> daten = echteDaten();

        ProduktRechner pr = new ProduktRechner();
        List<Produktergebnis> ergebnisse = pr.jeProdukt(daten);
        Map<String, Abweichung> abweichungen = new AbweichungsRechner().jeProdukt(daten);

        // Summe aller Einzeleffekte ueber alle Produkte
        BigDecimal ausZerlegung = new AbweichungsRechner()
                .summe(List.copyOf(abweichungen.values())).gesamt();

        // Gegenprobe aus dem Betriebsergebnis
        BigDecimal ausDeckungsbeitrag = pr.gesamtIst(ergebnisse).dbZwei()
                .subtract(pr.gesamtPlan(ergebnisse).dbZwei());

        assertEquals(0, ausZerlegung.subtract(ausDeckungsbeitrag).signum(),
                "Gesamt: Zerlegung " + ausZerlegung + " vs. " + ausDeckungsbeitrag);
    }

    /** Liest die echte CSV und laesst sie durch Validierung und Umwandlung laufen. */
    private static List<Datenzeile> echteDaten() throws IOException {
        Pruefprotokoll protokoll = new Validator()
                .pruefe(new CsvEinleser().lies(Path.of("controlling_rohdaten.csv")));

        return protokoll.verwertbareZeilen().stream()
                .map(Datenzeile::aus)
                .toList();
    }
}
