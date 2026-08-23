package de.leo.controlling.report;

import de.leo.controlling.abweichung.Abweichung;
import de.leo.controlling.abweichung.AbweichungsRechner;
import de.leo.controlling.abweichung.Wesentlichkeit;
import de.leo.controlling.model.Datenzeile;
import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Pruefprotokoll;
import de.leo.controlling.pruefung.Validator;
import de.leo.controlling.rechnung.ProduktRechner;
import de.leo.controlling.rechnung.Produktergebnis;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Setzt aus Rohzeilen das fertige {@link Berichtsmodell} zusammen.
 *
 * <p>Hier laeuft die ganze Pipeline an einer Stelle zusammen: validieren, umwandeln,
 * rechnen, zerlegen. Danach kann App sich auf Ein- und Ausgabe beschraenken.
 */
public final class BerichtsmodellBauer {

    /**
     * @param quelle     Dateiname fuer den Bericht
     * @param roh        alle eingelesenen Zeilen
     * @param erstelltAm Zeitstempel (uebergeben, damit Tests ihn festlegen koennen)
     */
    /** Mit den Standardschwellen (500 EUR / 5 %) - fuer Tests und einfache Aufrufe. */
    public Berichtsmodell baue(String quelle, List<Rohzeile> roh, LocalDateTime erstelltAm) {
        return baue(quelle, roh, erstelltAm, Wesentlichkeit.standard());
    }

    /**
     * @param wesentlichkeit die Schwellen fuer die Ampel - kommen aus der
     *                       Kommandozeile, deshalb als Parameter
     */
    public Berichtsmodell baue(String quelle, List<Rohzeile> roh,
                               LocalDateTime erstelltAm, Wesentlichkeit wesentlichkeit) {
 
        Pruefprotokoll protokoll = new Validator().pruefe(roh);
                  
        List<Datenzeile> daten = protokoll.verwertbareZeilen().stream().map(Datenzeile::aus).toList();
        
        ProduktRechner pr = new ProduktRechner();
        List<Produktergebnis> produkte = pr.jeProdukt(daten);
        int erwarteteMonate = (int) daten.stream().map(Datenzeile::monat).distinct().count();
    
        Map<String, Abweichung> abweichungen = new AbweichungsRechner().jeProdukt(daten);

        return new Berichtsmodell(
                quelle,
                erstelltAm,
                erwarteteMonate,
                roh,
                protokoll,
                produkte,
                abweichungen,
                pr.gesamtPlan(produkte),
                pr.gesamtIst(produkte),
                pr.jeProduktUndMonat(daten),
                pr.jeKostenstelle(daten),
                wesentlichkeit);
    }
}
