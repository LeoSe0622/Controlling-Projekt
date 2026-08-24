package de.leo.controlling.pruefung.regeln;

import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Schweregrad;
import de.leo.controlling.pruefung.Zeilenregel;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * V04 — monat ist nicht im Format YYYY-MM oder liegt ausserhalb 01..12.
 *
 * <p>Feuert bei Tippfehlern im Monat ("2025-13") und bei Dateien, die ein anderes
 * Datumsformat verwenden als YYYY-MM.
 */
public class MonatsformatRegel implements Zeilenregel {

    private static final String ID = "V04";

    @Override
    public List<Befund> pruefe(Rohzeile zeile) {
        String monat = zeile.monat();

        if (monat == null || monat.isBlank()) {
            return List.of();
        }

        try {
            YearMonth.parse(monat);
            return List.of();
        } catch (DateTimeParseException e) {
            return List.of(new Befund(
                    zeile.zeilennummer(),
                    "monat",
                    ID,
                    Schweregrad.FEHLER,
                    monat,
                    "Monat '" + monat + "' ist nicht im Format YYYY-MM (01-12)"
            ));
        }
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String bezeichnung() {
        return "Monatsformat";
    }
}
