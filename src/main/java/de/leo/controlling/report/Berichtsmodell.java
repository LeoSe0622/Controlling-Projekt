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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Alles, was ein Bericht braucht - und nichts, was mit seiner Darstellung zu tun hat.
 *
 * <p>Dieses Objekt kennt weder Excel noch die Konsole. Es weiss nicht, welche Spalte wo
 * steht, welche Farbe rot ist oder wie eine Zahl formatiert wird. Es haelt nur die Zahlen
 * und die Regeln, nach denen man sie liest.
 *
 * <p><b>Warum das der Muehe wert ist:</b> Ohne dieses Objekt muesste der Excel-Writer
 * dieselben Entscheidungen noch einmal treffen wie die Konsolenausgabe - welche Produkte,
 * welche Reihenfolge, welche Ampel. Zwei Stellen, dieselbe Logik, und sie laufen beim
 * ersten Feld auseinander, das man nur an einer Stelle ergaenzt. So gibt es EINE Wahrheit
 * und zwei Darstellungen davon.
 *
 * <p>{@code erstelltAm} wird uebergeben statt hier erzeugt: Ein Modell, das sich seine
 * eigene Uhrzeit holt, ist nicht testbar.
 *
 * @param quelle           Dateiname der eingelesenen CSV
 * @param erstelltAm       Zeitpunkt der Berichtserstellung
 * @param erwarteteMonate  wie viele VERSCHIEDENE Monate in den verwertbaren Daten stehen.
 *                         Der Name sagt "erwartet", gemessen wird "vorhanden" - fielen
 *                         saemtliche Zeilen eines Monats durch die Pruefung, saenke die
 *                         Erwartung stillschweigend mit. Solange die Datei den Zeitraum
 *                         vollstaendig abdeckt, ist das dieselbe Zahl.
 * @param alleZeilen       alle eingelesenen Rohzeilen (fuer den Rohdaten-Tab)
 * @param protokoll        Befunde und verwertbare Zeilen
 * @param produkte         Ergebnis je Produkt, alphabetisch
 * @param abweichungen     Zerlegung je Produkt, per Produktnamen abrufbar
 * @param gesamtPlan       Betriebsergebnis Plan
 * @param gesamtIst        Betriebsergebnis Ist
 * @param warnzeilen       welcher Teil davon aus beanstandeten Zeilen stammt
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
        Warnzeileneinfluss warnzeilen,
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

        return gesamtAbweichung().gesamt()
                .subtract(gesamtIst.dbZwei().subtract(gesamtPlan.dbZwei()));
    }

    /** Ob die Abstimmbruecke aufgeht. */
    public boolean brueckeGehtAuf() {
        return brueckenDifferenz().signum() == 0;
    }

    /**
     * Die Gesamtabweichung ohne die Zeilen, zu denen es eine Warnung gibt.
     *
     * <p>Exakt, nicht geschaetzt: Beide Summanden sind auf zwei Stellen gerundete
     * Betraege, ihre Differenz ist es also auch.
     */
    public BigDecimal abweichungOhneWarnzeilen() {

        return gesamtAbweichung().gesamt().subtract(warnzeilen.abweichung());
    }

    /**
     * Welcher Anteil der Gesamtabweichung aus beanstandeten Zeilen stammt
     * (1.0 = 100 %, Werte ueber 1 sind moeglich und heissen: die uebrigen Zeilen
     * zeigen in die andere Richtung).
     *
     * @return {@code null}, wenn es gar keine Gesamtabweichung gibt - ein Anteil an
     *         null ist nicht null, sondern undefiniert
     */
    public BigDecimal anteilDerWarnzeilen() {

        BigDecimal gesamt = gesamtAbweichung().gesamt();
        if (gesamt.signum() == 0) {
            return null;
        }
        return warnzeilen.abweichung().divide(gesamt, 4, RoundingMode.HALF_UP);
    }

    /**
     * Ob die beanstandeten Zeilen die Kernaussage des Berichts umdrehen.
     *
     * <p>Das schaerfste Kriterium, das es gibt: nicht "wie gross ist der Einfluss",
     * sondern "steht ohne diese Zeilen etwas anderes da". Ist das der Fall, darf der
     * Bericht seine Ueberschrift nicht unkommentiert lassen.
     */
    public boolean warnzeilenKippenDasErgebnis() {

        if (warnzeilen.zeilen() == 0) {
            return false;
        }
        return gesamtAbweichung().gesamt().signum() != abweichungOhneWarnzeilen().signum();
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
    public Schweregrad statusVon(int zeilennummer) {

        boolean hatFehler = protokoll.befunde().stream()
                .filter(b -> b.zeilennummer() == zeilennummer)
                .anyMatch(b -> b.grad() == Schweregrad.FEHLER);

        if (hatFehler) {
            return Schweregrad.FEHLER;
        }

        boolean hatWarnung = protokoll.befunde().stream()
                .filter(b -> b.zeilennummer() == zeilennummer)
                .anyMatch(b -> b.grad() == Schweregrad.WARNUNG);

        return hatWarnung ? Schweregrad.WARNUNG : null;
    }

    /** Produkte, denen Monate fehlen - fuer den Hinweis unter der Tabelle. */
    public List<Produktergebnis> unvollstaendigeProdukte() {

        return produkte.stream()
                .filter(p -> !p.vollstaendig(erwarteteMonate))
                .toList();
    }
}
