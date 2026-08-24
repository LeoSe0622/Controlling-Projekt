package de.leo.controlling.report;

import de.leo.controlling.abweichung.Abweichung;
import de.leo.controlling.abweichung.AbweichungsRechner;
import de.leo.controlling.abweichung.Wesentlichkeit;
import de.leo.controlling.model.Datenzeile;
import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Pruefprotokoll;
import de.leo.controlling.pruefung.Schweregrad;
import de.leo.controlling.pruefung.Validator;
import de.leo.controlling.rechnung.Deckungsbeitrag;
import de.leo.controlling.rechnung.DeckungsbeitragsRechner;
import de.leo.controlling.rechnung.ProduktRechner;
import de.leo.controlling.rechnung.Produktergebnis;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Setzt aus Rohzeilen das fertige {@link Berichtsmodell} zusammen.
 *
 * <p>Hier laeuft die ganze Pipeline an einer Stelle zusammen: validieren, umwandeln,
 * rechnen, zerlegen. Danach kann App sich auf Ein- und Ausgabe beschraenken.
 */
public final class BerichtsmodellBauer {

    /** Mit den Standardschwellen (500 EUR / 5 %) - fuer Tests und einfache Aufrufe. */
    public Berichtsmodell baue(String quelle, List<Rohzeile> roh, LocalDateTime erstelltAm) {
        return baue(quelle, roh, erstelltAm, Wesentlichkeit.standard());
    }

    /** Mit fest vorgegebenen Schwellen. */
    public Berichtsmodell baue(String quelle, List<Rohzeile> roh,
                               LocalDateTime erstelltAm, Wesentlichkeit wesentlichkeit) {
        return baue(quelle, roh, erstelltAm,
                wesentlichkeit.schwelleEuro(), wesentlichkeit.schwelleProzent());
    }

    /**
     * Der vollstaendige Weg.
     *
     * <p>{@code schwelleEuro} darf {@code null} sein - dann wird sie aus dem geplanten
     * Betriebsergebnis abgeleitet. Das geht erst HIER, weil dieses Ergebnis vorher noch
     * nicht feststeht: Die Kommandozeile kennt den Datenumfang nicht.
     *
     * @param quelle          Dateiname fuer den Bericht
     * @param roh             alle eingelesenen Zeilen
     * @param erstelltAm      Zeitstempel (uebergeben, damit Tests ihn festlegen koennen);
     *                        sein Monat ist zugleich der Bezugspunkt fuer Regel V09
     * @param schwelleEuro    absolute Ampel-Schwelle oder {@code null} fuer "ableiten"
     * @param schwelleProzent relative Ampel-Schwelle als Anteil (0.05 = 5 %)
     */
    public Berichtsmodell baue(String quelle, List<Rohzeile> roh, LocalDateTime erstelltAm,
                               BigDecimal schwelleEuro, BigDecimal schwelleProzent) {

        Pruefprotokoll protokoll =
                new Validator(YearMonth.from(erstelltAm)).pruefe(roh);

        List<Datenzeile> daten = protokoll.verwertbareZeilen().stream()
                .map(Datenzeile::aus)
                .toList();

        ProduktRechner pr = new ProduktRechner();
        List<Produktergebnis> produkte = pr.jeProdukt(daten);
        Deckungsbeitrag gesamtPlan = pr.gesamtPlan(produkte);
        Deckungsbeitrag gesamtIst = pr.gesamtIst(produkte);

        int erwarteteMonate = (int) daten.stream().map(Datenzeile::monat).distinct().count();

        Map<String, Abweichung> abweichungen = new AbweichungsRechner().jeProdukt(daten);

        Wesentlichkeit wesentlichkeit = schwelleEuro != null
                ? new Wesentlichkeit(schwelleEuro, schwelleProzent)
                : Wesentlichkeit.fuer(gesamtPlan.dbZwei(), schwelleProzent);

        return new Berichtsmodell(
                quelle,
                erstelltAm,
                erwarteteMonate,
                roh,
                protokoll,
                produkte,
                abweichungen,
                gesamtPlan,
                gesamtIst,
                warnzeileneinfluss(protokoll, daten),
                pr.jeProduktUndMonat(daten),
                pr.jeKostenstelle(daten),
                wesentlichkeit);
    }

    /**
     * Rechnet Plan und Ist noch einmal, aber nur ueber die Zeilen mit einer Warnung.
     *
     * <p>Ein zweiter Durchgang statt einer Kennzahl aus dem ersten: Der Einfluss dieser
     * Zeilen ist keine Summe von Teilergebnissen, sondern ein eigener Schnitt durch
     * dieselben Daten. Ihn im ersten Durchgang mitzufuehren hiesse, jede Aggregation
     * doppelt zu halten.
     *
     * <p>Gefiltert wird ueber die Zeilennummer, nicht ueber den Befund: Eine Zeile mit
     * zwei Warnungen darf nur einmal zaehlen, sonst waere ihr Betrag doppelt drin.
     */
    private static Warnzeileneinfluss warnzeileneinfluss(Pruefprotokoll protokoll,
                                                         List<Datenzeile> daten) {

        Set<Integer> mitWarnung = protokoll.befunde().stream()
                .filter(b -> b.grad() == Schweregrad.WARNUNG)
                .map(Befund::zeilennummer)
                .collect(Collectors.toSet());

        List<Datenzeile> betroffen = daten.stream()
                .filter(z -> mitWarnung.contains(z.zeilennummer()))
                .toList();

        DeckungsbeitragsRechner rechner = new DeckungsbeitragsRechner();

        return new Warnzeileneinfluss(
                betroffen.size(),
                rechner.summe(betroffen.stream().map(rechner::plan).toList()),
                rechner.summe(betroffen.stream().map(rechner::ist).toList()));
    }
}
