package de.leo.controlling.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests fuer die Ableitung des Ausgabepfads.
 *
 * <p>Diese Methode wird in Phase 8 der Pfad, auf den alles zeigt: Beim Reinziehen einer
 * CSV ist das Arbeitsverzeichnis irgendein Windows-Systemordner, der Bericht muss also
 * NEBEN der Eingabedatei landen. Liegt die Methode falsch, sucht der Benutzer sein
 * Ergebnis - und findet es nicht.
 */
class AusgabepfadTest {

    @TempDir
    Path tempDir;

    @Test
    void haengtSuffixAnUndTauschtDieEndung() {
        Path eingabe = tempDir.resolve("maerz.csv");

        Path ziel = Berichtskonfiguration.ausgabepfadFuer(eingabe);

        assertEquals("maerz_Monatsbericht.xlsx", ziel.getFileName().toString());
    }

    @Test
    void berichtLandetImOrdnerDerEingabedatei() {
        // Der eigentliche Zweck: nicht im Arbeitsverzeichnis, sondern dort,
        // wo auch die Daten liegen.
        Path eingabe = tempDir.resolve("unterordner").resolve("maerz.csv");

        Path ziel = Berichtskonfiguration.ausgabepfadFuer(eingabe);

        assertEquals(eingabe.toAbsolutePath().getParent(), ziel.getParent());
    }

    @Test
    void kommtOhneDateiendungZurecht() {
        Path ziel = Berichtskonfiguration.ausgabepfadFuer(tempDir.resolve("maerz"));

        assertEquals("maerz_Monatsbericht.xlsx", ziel.getFileName().toString());
    }

    @Test
    void schneidetNurDieLetzteEndungAb() {
        // "export.2025.csv" - lastIndexOf('.') trifft den letzten Punkt,
        // die Versionsnummer im Namen bleibt erhalten.
        Path ziel = Berichtskonfiguration.ausgabepfadFuer(tempDir.resolve("export.2025.csv"));

        assertEquals("export.2025_Monatsbericht.xlsx", ziel.getFileName().toString());
    }

    @Test
    void behandeltVersteckteDateienRichtig() {
        // ".daten" faengt mit einem Punkt an: lastIndexOf('.') ist 0.
        // Deshalb steht in der Methode "punkt > 0" und nicht "punkt >= 0" -
        // sonst bliebe vom Namen nichts uebrig ausser "_Monatsbericht.xlsx".
        Path ziel = Berichtskonfiguration.ausgabepfadFuer(tempDir.resolve(".daten"));

        assertEquals(".daten_Monatsbericht.xlsx", ziel.getFileName().toString());
    }

    @Test
    void machtAusRelativemPfadEinenAbsoluten() {
        // Beim Reinziehen kann ein relativer Pfad ankommen. Der Bericht muss
        // trotzdem eindeutig verortet sein.
        Path ziel = Berichtskonfiguration.ausgabepfadFuer(Path.of("irgendeine.csv"));

        assertEquals(true, ziel.isAbsolute());
    }
}
