package de.leo.controlling.config;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests fuer das Einlesen der Kommandozeile.
 *
 * <p>Hier lohnt sich Sorgfalt besonders: Argumente kommen von MENSCHEN. Alles, was
 * schiefgehen kann, geht irgendwann schief — und der Benutzer sieht nur die Meldung,
 * nicht den Code. Ein Tippfehler darf nie wie ein Programmabsturz aussehen.
 */
class BerichtskonfigurationTest {

    @Test
    void oeffnetDenBerichtStandardmaessig() {
        // Wer das Programm von Hand aufruft, will das Ergebnis sehen.
        assertTrue(Berichtskonfiguration.ausArgumenten(new String[]{"daten.csv"}).oeffnen());
    }

    @Test
    void keinOeffnenSchaltetAb() {
        // Die Exit-Codes gibt es fuer Automatisierung. Ein Skript, das nachts
        // laeuft, darf kein Excel aufmachen - sonst blockiert es bis zum Morgen.
        Berichtskonfiguration k = Berichtskonfiguration.ausArgumenten(
                new String[]{"daten.csv", "--kein-oeffnen"});

        assertFalse(k.oeffnen());
    }

    @Test
    void keinOeffnenVerschlucktDasNaechsteArgumentNicht() {
        // --kein-oeffnen hat KEINEN Wert dahinter. Ein i++ im case-Zweig wuerde
        // das folgende Argument ueberspringen - hier waere die Schwelle dann
        // stillschweigend die Vorgabe.
        Berichtskonfiguration k = Berichtskonfiguration.ausArgumenten(
                new String[]{"daten.csv", "--kein-oeffnen", "--schwelle-eur", "1000"});

        assertFalse(k.oeffnen());
        assertEquals(0, k.wesentlichkeit().schwelleEuro().compareTo(new BigDecimal("1000")));
    }

    @Test
    void liestEingabeMitOption() {
        Berichtskonfiguration k = Berichtskonfiguration.ausArgumenten(
                new String[]{"--input", "daten.csv"});

        assertEquals("daten.csv", k.eingabe().getFileName().toString());
    }

    @Test
    void liestEingabeAuchOhneOption() {
        // DER Test fuer Phase 8: Beim Ziehen einer Datei auf eine .bat uebergibt
        // Windows den nackten Pfad, ohne "--input" davor.
        Berichtskonfiguration k = Berichtskonfiguration.ausArgumenten(
                new String[]{"daten.csv"});

        assertEquals("daten.csv", k.eingabe().getFileName().toString());
    }

    @Test
    void leitetAusgabepfadAbWennKeinerAngegebenIst() {
        Berichtskonfiguration k = Berichtskonfiguration.ausArgumenten(
                new String[]{"--input", "daten/maerz.csv"});

        assertEquals("maerz_Monatsbericht.xlsx", k.ausgabe().getFileName().toString());
    }

    @Test
    void uebernimmtAngegebenenAusgabepfad() {
        Berichtskonfiguration k = Berichtskonfiguration.ausArgumenten(
                new String[]{"--input", "daten.csv", "--output", "berichte/report.xlsx"});

        assertEquals("report.xlsx", k.ausgabe().getFileName().toString());
    }

    @Test
    void ohneSchwellenGeltenDieVorgaben() {
        Berichtskonfiguration k = Berichtskonfiguration.ausArgumenten(
                new String[]{"daten.csv"});

        assertEquals(new BigDecimal("500.00"), k.wesentlichkeit().schwelleEuro());
        assertEquals(new BigDecimal("0.05"), k.wesentlichkeit().schwelleProzent());
    }

    @Test
    void rechnetProzentangabeUm() {
        // Der Benutzer tippt 5, Wesentlichkeit erwartet 0.05. Wuerde die Umrechnung
        // fehlen, waere die Schwelle 500 Prozent - und die Ampel bliebe immer gruen.
        Berichtskonfiguration k = Berichtskonfiguration.ausArgumenten(
                new String[]{"daten.csv", "--schwelle-prozent", "5"});

        assertEquals(0, k.wesentlichkeit().schwelleProzent()
                .compareTo(new BigDecimal("0.05")));
    }

    @Test
    void uebernimmtEuroSchwelle() {
        Berichtskonfiguration k = Berichtskonfiguration.ausArgumenten(
                new String[]{"daten.csv", "--schwelle-eur", "1000"});

        assertEquals(0, k.wesentlichkeit().schwelleEuro()
                .compareTo(new BigDecimal("1000")));
    }

    @Test
    void meldetFehlendeEingabedatei() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Berichtskonfiguration.ausArgumenten(new String[]{}));

        assertTrue(ex.getMessage().toLowerCase().contains("eingabe"),
                "die Meldung muss sagen, WAS fehlt: " + ex.getMessage());
    }

    @Test
    void meldetUnbekannteOption() {
        // Tippfehler: --inpu statt --input
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Berichtskonfiguration.ausArgumenten(
                        new String[]{"--inpu", "daten.csv"}));

        assertTrue(ex.getMessage().contains("--inpu"),
                "die Meldung muss die falsche Option NENNEN: " + ex.getMessage());
    }

    @Test
    void meldetFehlendenWertNachOption() {
        // "--input" am Zeilenende, ohne Pfad dahinter. Ohne Pruefung gaebe es eine
        // ArrayIndexOutOfBoundsException - der Benutzer saehe einen Absturz statt
        // eines Hinweises.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Berichtskonfiguration.ausArgumenten(new String[]{"--input"}));

        assertTrue(ex.getMessage().contains("--input"), ex.getMessage());
    }

    @Test
    void meldetUnbrauchbareZahl() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Berichtskonfiguration.ausArgumenten(
                        new String[]{"daten.csv", "--schwelle-eur", "abc"}));

        assertTrue(ex.getMessage().contains("abc"),
                "die Meldung muss den falschen Wert zeigen: " + ex.getMessage());
    }

    @Test
    void hilfetextNenntAlleOptionen() {
        // Eine Hilfe, die eine Option verschweigt, ist schlimmer als keine.
        for (String option : new String[]{
                "--input", "--output", "--schwelle-eur", "--schwelle-prozent", "--help"}) {
            assertTrue(Berichtskonfiguration.HILFE.contains(option),
                    "Hilfetext erwaehnt " + option + " nicht");
        }
    }
}
