package de.leo.controlling.rechnung;

import de.leo.controlling.model.Datenzeile;

import java.math.BigDecimal;
import java.time.YearMonth;
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

    /**
     * Ergebnis je Produkt UND Monat - die Grundlage fuer die Zeitreihe.
     *
     * <p>Gruppiert wird, obwohl es nach der Validierung je (Produkt, Monat) nur
     * eine Zeile geben kann: Die Dublettenregel sorgt dafuer, nicht diese Methode.
     * Wer sich hier auf die Eindeutigkeit verlaesst, rechnet still falsch, sobald
     * jemand V07 abschaltet oder die Daten anders geschnitten sind.
     *
     * @return sortiert nach Produkt, darin nach Monat
     */
    public List<Monatsergebnis> jeProduktUndMonat(List<Datenzeile> zeilen) {

        Map<String, Map<YearMonth, List<Datenzeile>>> gruppen = new TreeMap<>();
        for (Datenzeile z : zeilen) {
            gruppen.computeIfAbsent(z.produkt(), k -> new TreeMap<>())
                    .computeIfAbsent(z.monat(), k -> new ArrayList<>())
                    .add(z);
        }

        List<Monatsergebnis> ergebnis = new ArrayList<>();
        for (Map.Entry<String, Map<YearMonth, List<Datenzeile>>> proProdukt : gruppen.entrySet()) {
            for (Map.Entry<YearMonth, List<Datenzeile>> proMonat : proProdukt.getValue().entrySet()) {

                List<Deckungsbeitrag> plan = new ArrayList<>();
                List<Deckungsbeitrag> ist = new ArrayList<>();
                for (Datenzeile z : proMonat.getValue()) {
                    plan.add(rechner.plan(z));
                    ist.add(rechner.ist(z));
                }

                ergebnis.add(new Monatsergebnis(proProdukt.getKey(), proMonat.getKey(),
                        rechner.summe(plan), rechner.summe(ist)));
            }
        }
        return ergebnis;
    }

    /**
     * Ergebnis je Kostenstelle - dieselbe Gruppierung wie {@link #jeProdukt}, nur
     * mit einem anderen Schluessel.
     *
     * <p>Zur Aussagekraft siehe {@link Kostenstellenergebnis}: Der DB I laesst sich
     * einer Region sauber zurechnen, der DB II nur eingeschraenkt.
     *
     * @return alphabetisch nach Kostenstelle
     */
    public List<Kostenstellenergebnis> jeKostenstelle(List<Datenzeile> zeilen) {

        Map<String, List<Datenzeile>> nachStelle = new TreeMap<>();
        for (Datenzeile z : zeilen) {
            nachStelle.computeIfAbsent(z.kostenstelle(), k -> new ArrayList<>()).add(z);
        }

        List<Kostenstellenergebnis> ergebnis = new ArrayList<>();
        for (Map.Entry<String, List<Datenzeile>> e : nachStelle.entrySet()) {
            List<Deckungsbeitrag> plan = new ArrayList<>();
            List<Deckungsbeitrag> ist = new ArrayList<>();
            for (Datenzeile z : e.getValue()) {
                plan.add(rechner.plan(z));
                ist.add(rechner.ist(z));
            }
            ergebnis.add(new Kostenstellenergebnis(e.getKey(),
                    rechner.summe(plan), rechner.summe(ist), e.getValue().size()));
        }
        return ergebnis;
    }

    /** Zaehlt die VERSCHIEDENEN Monate einer Gruppe. */
    private static int monate(List<Datenzeile> gruppe) {     

        return (int) gruppe.stream().map(Datenzeile::monat).distinct().count();
    }
}
