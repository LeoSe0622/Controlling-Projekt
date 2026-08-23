package de.leo.controlling.config;

import de.leo.controlling.abweichung.Wesentlichkeit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;

/**
 * Alles, was der Aufrufer entscheiden darf — aus den Kommandozeilenargumenten gelesen.
 *
 * <p>Diese Klasse trennt zwei Dinge, die leicht verschwimmen: WAS das Programm tun soll
 * (steht hier) und WIE es das tut (steht in den Modulen). Sie kennt weder CSV noch Excel,
 * nur Pfade und Schwellen.
 *
 * @param eingabe        die zu lesende CSV
 * @param ausgabe        die zu schreibende .xlsx
 * @param wesentlichkeit die Schwellen fuer die Ampel
 * @param oeffnen        ob der fertige Bericht im Standardprogramm geoeffnet wird.
 *                       Fuer Menschen sinnvoll, fuer Automatisierung nicht - ein
 *                       Skript, das nachts laeuft, soll kein Excel aufmachen.
 */
public record Berichtskonfiguration(
        Path eingabe,
        Path ausgabe,
        Wesentlichkeit wesentlichkeit,
        boolean oeffnen
) {

    /** Wird bei --help oder ohne Argumente ausgegeben. */
    public static final String HILFE = """
            Controlling-Monatsbericht

            Aufruf:
              controlling-report <datei.csv>
              controlling-report --input <datei.csv> [Optionen]

            Optionen:
              --input <pfad>              die einzulesende CSV (Pflicht)
              --output <pfad>             Zieldatei; ohne Angabe neben der Eingabe
              --schwelle-eur <betrag>     Ampel-Schwelle in Euro (Vorgabe: 500)
              --schwelle-prozent <zahl>   Ampel-Schwelle in Prozent (Vorgabe: 5)
              --kein-oeffnen              Bericht nicht automatisch oeffnen
              --help                      diese Hilfe

            Exit-Codes:
              0  sauber
              1  nur Warnungen
              2  Fehlerzeilen in den Daten
              3  Abbruch (Datei unlesbar oder Argumente falsch)
            """;

    /**
     * Liest die Argumente.
     *
     * <p>Der Eingabepfad wird auch OHNE {@code --input} angenommen: Beim Ziehen einer
     * Datei auf eine .bat uebergibt Windows den nackten Pfad. Alles, was mit {@code --}
     * beginnt und unbekannt ist, gilt dagegen als Tippfehler.
     *
     * <p>{@code --schwelle-prozent} wird hier von 5 auf 0.05 umgerechnet, an genau einer
     * Stelle. Bliebe die Umrechnung aus, waere die Schwelle 500 Prozent und die Ampel
     * dauerhaft gruen - ein Fehler, der nichts kaputtmacht und alles wertlos.
     *
     * <p>{@code --kein-oeffnen} hat als einzige Option KEINEN Wert dahinter und schiebt
     * den Index deshalb nicht weiter. Ein {@code i++} wuerde das folgende Argument
     * verschlucken.
     *
     * @throws IllegalArgumentException bei unbekannten Optionen, fehlenden Werten oder
     *                                  unbrauchbaren Zahlen — die Meldung ist fuer den
     *                                  BENUTZER gedacht, nicht fuer den Entwickler
     */
    public static Berichtskonfiguration ausArgumenten(String[] args) {

        Path eingabe = null;
        Path ausgabe = null;
        BigDecimal schwelleEuro = new BigDecimal("500.00");
        BigDecimal schwelleProzent = new BigDecimal("0.05");

        boolean oeffnen = true;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--input" -> {
                    eingabe = Path.of(wert(args, i));
                    i++;
                }
                case "--output" -> {
                    ausgabe = Path.of(wert(args, i));
                    i++;
                }
                case "--schwelle-eur" -> {
                    schwelleEuro = zahl(args, i);
                    i++;
                }
                case "--schwelle-prozent" -> {
                    schwelleProzent = zahl(args, i)
                            .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                    i++;
                }
                case "--kein-oeffnen" -> {
                    oeffnen = false;
                }
                case "--help" -> {
                    System.out.println(HILFE);
                    System.exit(0);
                }
                default -> {
                    if (args[i].startsWith("--")) {
                        throw new IllegalArgumentException(
                                "Unbekannte Option: '" + args[i] + "'.");
                    }
                    if (eingabe != null) {
                        throw new IllegalArgumentException(
                                "Mehrere Eingabedateien angegeben: '" + args[i] + "'.");
                    }
                    eingabe = Path.of(args[i]);
                }
            }
        }

        if (eingabe == null) {
            throw new IllegalArgumentException("Keine Eingabedatei angegeben.");
        }

        if (ausgabe == null) {
            ausgabe = ausgabepfadFuer(eingabe);
        }

        return new Berichtskonfiguration(eingabe, ausgabe,
                new Wesentlichkeit(schwelleEuro, schwelleProzent), oeffnen);
    }

    /**
     * Leitet den Ausgabepfad aus der Eingabedatei ab:
     * {@code daten/maerz.csv} wird zu {@code daten/maerz_Monatsbericht.xlsx}.
     *
     * <p>Der Bericht landet im Ordner der EINGABEDATEI, nicht im Arbeitsverzeichnis.
     * Beim Ziehen einer CSV auf eine .bat waere das sonst irgendein Windows-Systemordner,
     * und der Benutzer sucht sein Ergebnis.
     */
    static Path ausgabepfadFuer(Path eingabe) {
        String name = eingabe.getFileName().toString();

        int punkt = name.lastIndexOf('.');
        String ohneEndung = punkt > 0 ? name.substring(0, punkt) : name;

        Path ordner = eingabe.toAbsolutePath().getParent();
        return ordner.resolve(ohneEndung + "_Monatsbericht.xlsx");
    }

    /**
     * Holt den Wert NACH einer Option — oder wirft, wenn keiner da ist.
     *
     * <p>{@code --input} am Ende der Zeile ohne Pfad dahinter ist ein haeufiger Tippfehler.
     * Ohne diese Pruefung gibt es eine ArrayIndexOutOfBoundsException, und der Benutzer
     * sieht einen Programmabsturz statt eines Hinweises.
     *
     * <p>Die Methode HOLT nur - sie veraendert das uebergebene Array nicht. Eine Methode,
     * die dem Namen nach etwas liest und dabei heimlich schreibt, ist an der Aufrufstelle
     * nicht als solche zu erkennen.
     */
    private static String wert(String[] args, int i) {

        if (i + 1 >= args.length) {
            throw new IllegalArgumentException("Zu '" + args[i] + "' fehlt der Wert.");
        }

        String wert = args[i + 1].trim();
        if (wert.isEmpty()) {
            throw new IllegalArgumentException("Zu '" + args[i] + "' fehlt der Wert.");
        }
        return wert;
    }

    /**
     * Liest einen Zahlenwert nach einer Option.
     *
     * <p>Bewusst NICHT {@code Zahlen.parse()}: Das liefert bei Unsinn ein {@code null} und
     * schweigt. In der Validierung ist das richtig — dort sind kaputte Werte erwartete
     * DATEN, ueber die berichtet wird. Hier sind es BENUTZEREINGABEN, und ein Tippfehler
     * in {@code --schwelle-eur abc} darf nicht stillschweigend die Vorgabe stehen lassen.
     */
    private static BigDecimal zahl(String[] args, int i) {
        String wert = wert(args, i);
        try {
            return new BigDecimal(wert);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    args[i] + " erwartet eine Zahl, war aber '" + wert + "'.", e);
        }
    }
}
