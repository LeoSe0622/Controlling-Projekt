package de.leo.controlling.io;

import de.leo.controlling.model.Rohzeile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Liest die Rohdaten-CSV ein und liefert eine {@link Rohzeile} je Datenzeile.
 *
 * <p>Diese Klasse verändert nichts und prüft nichts: kein {@code trim()}, kein Parsen,
 * keine Plausibilitätsprüfung. Was in der Datei stand, steht auch in der Rohzeile —
 * inklusive leerer Felder. Geprüft wird erst in der Validierung (Phase 2).
 *
 * <p>Sie stürzt bei keiner Zeile ab. Zeilen mit zu wenig Spalten werden mit leeren
 * Feldern aufgefüllt, statt eine {@code ArrayIndexOutOfBoundsException} zu werfen.
 */
public class CsvEinleser {

    /**
     * Liest die Datei und liefert eine Rohzeile je nicht-leerer Datenzeile.
     *
     * <p>Das {@code -1} in {@code split(",", -1)} ist keine Feinheit: Ohne den Parameter
     * verwirft Java am Ende stehende leere Felder. Eine Zeile mit leerer letzter Spalte
     * haette dann acht statt neun Spalten, und Regel V01 wuerde faelschlich anschlagen.
     *
     * <p>Die Zeilennummer ist {@code i + 1}, weil die Kopfzeile im Editor Zeile 1 ist.
     * Stimmt sie nicht, zeigen saemtliche Fehlermeldungen des Programms auf die falsche
     * Zeile.
     *
     * @param pfad Pfad zur CSV-Datei
     * @return eine Rohzeile je nicht-leerer Datenzeile, Kopfzeile ausgenommen
     * @throws IOException wenn die Datei nicht gelesen werden kann
     */
    public List<Rohzeile> lies(Path pfad) throws IOException {
        List<String> zeilen = Files.readAllLines(pfad, StandardCharsets.UTF_8);
        List<Rohzeile> ergebnis = new ArrayList<>();

        for (int i = 1; i < zeilen.size(); i++) {
            String text = zeilen.get(i);

            if (text.isBlank()) {
                continue;
            }

            String[] felder = text.split(",", -1);

            ergebnis.add(new Rohzeile(
                    i + 1,
                    felder.length,
                    feld(felder, 0),
                    feld(felder, 1),
                    feld(felder, 2),
                    feld(felder, 3),
                    feld(felder, 4),
                    feld(felder, 5),
                    feld(felder, 6),
                    feld(felder, 7),
                    feld(felder, 8)
            ));
        }

        return ergebnis;
    }

    /** Liefert das Feld an der Position — oder "", wenn die Zeile so viele Spalten nicht hat. */
    private static String feld(String[] felder, int index) {
        if (index < 0 || index >= felder.length) {
            return "";
        }
        return felder[index];
    }
}
