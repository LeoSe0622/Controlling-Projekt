package de.leo.controlling.report;

import de.leo.controlling.abweichung.Abweichung;
import de.leo.controlling.abweichung.AbweichungsRechner;
import de.leo.controlling.abweichung.Ampel;
import de.leo.controlling.abweichung.Wesentlichkeit;
import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Pruefprotokoll;
import de.leo.controlling.pruefung.Schweregrad;
import de.leo.controlling.rechnung.Deckungsbeitrag;
import de.leo.controlling.rechnung.Kostenstellenergebnis;
import de.leo.controlling.rechnung.Monatsergebnis;
import de.leo.controlling.rechnung.Produktergebnis;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Alles, was ein Bericht braucht — und nichts, was mit seiner Darstellung zu tun hat.
 *
 * <p>Dieses Objekt kennt weder Excel noch die Konsole. Es weiss nicht, welche Spalte wo
 * steht, welche Farbe rot ist oder wie eine Zahl formatiert wird. Es haelt nur die Zahlen
 * und die Regeln, nach denen man sie liest.
 *
 * <p><b>Warum das der Muehe wert ist:</b> Ohne dieses Objekt muesste der Excel-Writer
 * dieselben Entscheidungen noch einmal treffen wie die Konsolenausgabe — welche Produkte,
 * welche Reihenfolge, welche Ampel. Zwei Stellen, dieselbe Logik, und sie laufen beim
 * ersten Feld auseinander, das man nur an einer Stelle ergaenzt. So gibt es EINE Wahrheit
 * und zwei Darstellungen davon.
 *
 * <p>{@code erstelltAm} wird uebergeben statt hier erzeugt: Ein Modell, das sich seine
 * eigene Uhrzeit holt, ist nicht testbar.
 *
 * @param quelle           Dateiname der eingelesenen CSV
 * @param erstelltAm       Zeitpunkt der Berichtserstellung
 * @param erwarteteMonate  wie viele Monate der Datensatz umfasst
 * @param alleZeilen       alle eingelesenen Rohzeilen (fuer den Rohdaten-Tab)
 * @param protokoll        Befunde und verwertbare Zeilen
 * @param produkte         Ergebnis je Produkt, alphabetisch
 * @param abweichungen     Zerlegung je Produkt, per Produktnamen abrufbar
 * @param gesamtPlan       Betriebsergebnis Plan
 * @param gesamtIst        Betriebsergebnis Ist
 * @param wesentlichkeit   die Schwellen fuer die Ampel
 */
public record Berichtsmodell(
        String quelle,
        LocalDateTime erstelltAm,
        int erwarteteMonate,
        List<Rohzeile> alleZeilen,
        Pruefprotokoll protokoll,
        List<Produktergebnis> produkte,
        Map<String, Abweichung> abweichungen,
        Deckungsbeitrag gesamtPlan,
        Deckungsbeitrag gesamtIst,
        List<Monatsergebnis> zeitreihe,
        List<Kostenstellenergebnis> kostenstellen,
        Wesentlichkeit wesentlichkeit
) {

    /** Die Summe aller Abweichungseffekte ueber alle Produkte. */
    public Abweichung gesamtAbweichung() {

        return new AbweichungsRechner().summe(List.copyOf(abweichungen.values()));
    }

    /**
     * Die Differenz zwischen den beiden Rechenwegen: Zerlegung minus Deckungsbeitraege.
     * Null bedeutet, die Abstimmbruecke geht auf.
     */
    public BigDecimal brueckenDifferenz() {

    return gesamtAbweichung().gesamt().subtract(gesamtIst.dbZwei().subtract(gesamtPlan.dbZwei()));
    }

    /** Ob die Abstimmbruecke aufgeht. */
    public boolean brueckeGehtAuf() {
        if (brueckenDifferenz().signum() == 0) {
            return true;
        }
        return false;
    }

    /** Die Ampelstufe eines Produkts, gemessen an seinem Plan-DB-II. */
    public Ampel ampelFuer(Produktergebnis produkt) {
        return wesentlichkeit.bewerte(
                abweichungen.get(produkt.produkt()).gesamt(),
                produkt.plan().dbZwei()
        );
    }

    /**
     * Der schwerste Befund zu einer Rohzeile - oder {@code null}, wenn die Zeile
     * sauber ist. Fuer die Statusspalte im Rohdaten-Tab.
     *
     * <p>Eine Zeile kann mehrere Befunde haben. Angezeigt wird der schwerste:
     * Wer FEHLER und WARNUNG hat, ist ein FEHLER.
     */
    public Schweregrad.Grad statusVon(int zeilennummer) {

        // Erst nach FEHLERN suchen: Wer Fehler UND Warnung hat, ist ein Fehler.
        // anyMatch hoert beim ersten Treffer auf - es muessen nicht alle
        // Befunde durchlaufen werden.
        boolean hatFehler = protokoll.befunde().stream()
                .filter(b -> b.Zeilennummer() == zeilennummer)
                .anyMatch(b -> b.grad() == Schweregrad.Grad.Fehler);

        if (hatFehler) {
            return Schweregrad.Grad.Fehler;
        }

        boolean hatWarnung = protokoll.befunde().stream()
                .filter(b -> b.Zeilennummer() == zeilennummer)
                .anyMatch(b -> b.grad() == Schweregrad.Grad.Warnung);

        return hatWarnung ? Schweregrad.Grad.Warnung : null;
    }

    /** Produkte, denen Monate fehlen — fuer den Hinweis unter der Tabelle. */
    public List<Produktergebnis> unvollstaendigeProdukte() {

        // filter sammelt ALLE passenden Elemente ein. Ein return in einer
        // Schleife wuerde beim ersten unvollstaendigen Produkt aufhoeren -
        // und Produkt B, dem drei Monate fehlen, bliebe unkommentiert.
        return produkte.stream()
                .filter(p -> !p.vollstaendig(erwarteteMonate))
                .toList();
    }
}
