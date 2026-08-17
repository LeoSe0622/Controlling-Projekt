package de.leo.controlling;

import de.leo.controlling.io.CsvEinleser;
import de.leo.controlling.model.Rohzeile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Einstiegspunkt. Verdrahtet spaeter die ganze Pipeline
 * (Einlesen -> Validieren -> Rechnen -> Report).
 * Heute: nur Einlesen und anzeigen.
 */
public class App {

    public static void main(String[] args) {

        // pfad steht bewusst VOR dem try: der catch-Block braucht ihn noch fuer die Meldung.
        Path pfad = Path.of("controlling_rohdaten.csv");

        try {
            CsvEinleser einleser = new CsvEinleser();
            List<Rohzeile> roh = einleser.lies(pfad);
        
            System.out.println("Anzahl:" + roh.size());
            
            List<Rohzeile> liste = roh.subList(0, Math.min(3, roh.size()));
            liste.forEach(System.out::println);
            
            roh.stream().filter(r -> r.zeilennummer() == 22)
                    .findFirst()
                    .ifPresentOrElse(
                            r -> System.out.println("Zeile 22: " + r),
                            () -> System.out.println("Zeile 22 nicht gefunden")
                    );
        } catch (IOException e) {
            System.err.println("Datei konnte nicht gelesen werden: " + pfad.toAbsolutePath());
            System.err.println("Grund: " + e.getMessage());
            System.exit(3);
        }
    }
}
