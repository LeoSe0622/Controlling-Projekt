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
 * <p>Auf den echten Daten feuert sie nie — alle Monate sind 2025-01 bis 2025-12.
 */
public class MonatsformatRegel implements Zeilenregel {

    private static final String ID = "V04";

    @Override
    public List<Befund> pruefe(Rohzeile zeile) {
        String monat = zeile.monat();

        // Schweigegrundsatz: Ein leeres Pflichtfeld ist V02s Thema, nicht unseres.
        // Muss VOR dem Parsen stehen - YearMonth.parse("") wuerde sonst eine
        // DateTimeParseException werfen und wir wuerden faelschlich einen
        // Formatfehler melden.
        if (monat == null || monat.isBlank()) {
            return List.of();
        }

        try {
            YearMonth.parse(monat);
            return List.of();          // hat geklappt -> Monat ist gueltig
        } catch (DateTimeParseException e) {
            return List.of(new Befund(
                    zeile.zeilennummer(),
                    "monat",
                    ID,
                    Schweregrad.Grad.Fehler,
                    monat,
                    "Monat '" + monat + "' ist nicht im Format YYYY-MM (01-12)"
            ));
        }
    }
}
