package de.leo.controlling.rechnung;

import de.leo.controlling.model.Datenzeile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Fasst Datenzeilen zu einem Ergebnis je Produkt zusammen.
 *
 * <p>Der {@link DeckungsbeitragsRechner} rechnet EINE Zeile. Diese Klasse gruppiert nach
 * Produkt, rechnet jede Zeile und summiert die Monate auf.
 */
public final class ProduktRechner {

    private final DeckungsbeitragsRechner rechner = new DeckungsbeitragsRechner();

    /**
     * @param zeilen die verwertbaren Zeilen (ohne FEHLER)
     * @return ein Ergebnis je Produkt, alphabetisch sortiert
     */
    public List<Produktergebnis> jeProdukt(List<Datenzeile> zeilen) {
        List<Produktergebnis> ergebnis = new ArrayList<>();

        Map<String, List<Datenzeile>> nachProdukt = new TreeMap<>();
        for (Datenzeile z : zeilen) {
            nachProdukt.computeIfAbsent(z.produkt(), k -> new ArrayList<>()).add(z);
        }
        
        for(Map.Entry<String, List<Datenzeile>> e : nachProdukt.entrySet()) {
            List<Deckungsbeitrag> planTeile = new ArrayList<>();
            List<Deckungsbeitrag> istTeile = new ArrayList<>();
            for (Datenzeile z : e.getValue()) {
                planTeile.add(rechner.plan(z));
                istTeile.add(rechner.ist(z));
            }
            
            int monate = monate(e.getValue());

           ergebnis.add(new Produktergebnis(e.getKey(), rechner.summe(planTeile), rechner.summe(istTeile), monate));
        }
        return ergebnis.stream()
                .sorted((a, b) -> a.produkt().compareTo(b.produkt()))
                .collect(Collectors.toList());
    }

    /**
     * Betriebsergebnis der Plan-Seite: die Summe der DB II ueber ALLE Produkte.
     *
     * <p>Weil die CSV keine Unternehmensfixkosten liefert, ist die Summe der DB II
     * hier bereits das Betriebsergebnis. Kaemen spaeter Verwaltungs- oder
     * Vertriebsfixkosten dazu, waeren sie hier abzuziehen (DB III).
     *
     * <p><b>Achtung beim Lesen des Ergebnisses:</b> Der summierte Deckungsbeitrag
     * enthaelt auch ein Feld {@code menge}. Ueber verschiedene Produkte hinweg ist
     * das bedeutungslos - 2.000 Stueck Produkt C plus 350 Stueck Produkt D ergeben
     * keine sinnvolle Zahl. Nur die Geldbetraege duerfen aus dieser Summe in den
     * Bericht. Eine Datenstruktur kann technisch summierbar sein und fachlich
     * trotzdem Unsinn ergeben.
     */
    public Deckungsbeitrag gesamtPlan(List<Produktergebnis> ergebnisse) {

        return rechner.summe(ergebnisse.stream()
                .map(Produktergebnis::plan)
                .toList());
    }

    /** Betriebsergebnis der Ist-Seite, analog zu {@link #gesamtPlan(List)}. */
    public Deckungsbeitrag gesamtIst(List<Produktergebnis> ergebnisse) {

        return rechner.summe(ergebnisse.stream()
                .map(Produktergebnis::ist)
                .toList());
    }

    /** Zaehlt die VERSCHIEDENEN Monate einer Gruppe. */
    private static int monate(List<Datenzeile> gruppe) {     

        return (int) gruppe.stream().map(Datenzeile::monat).distinct().count();
    }
}
