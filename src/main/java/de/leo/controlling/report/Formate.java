package de.leo.controlling.report;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Die Zellformate einer Arbeitsmappe — <b>einmal</b> angelegt, ueberall wiederverwendet.
 *
 * <p><b>Das ist die wichtigste Regel beim Arbeiten mit POI.</b> Ein {@code CellStyle}
 * gehoert zur Arbeitsmappe, nicht zur Zelle. Wer in einer Schleife fuer jede Zelle
 * {@code workbook.createCellStyle()} aufruft, erzeugt bei 500 Zeilen 3.000 Formate.
 * Eine xlsx-Datei vertraegt rund 64.000 davon — danach wirft POI, und schon vorher
 * meldet Excel beim Oeffnen "unlesbarer Inhalt" und repariert die Datei.
 *
 * <p>Deshalb: Formate hier im Konstruktor anlegen, in den Schreibmethoden nur noch
 * zuweisen. Diese Klasse existiert allein, um diese Regel schwer zu brechen.
 */
final class Formate {

    final CellStyle titel;
    final CellStyle kopfzeile;
    final CellStyle beschriftung;
    final CellStyle geld;
    final CellStyle prozent;
    final CellStyle fehler;
    final CellStyle warnung;

    /** Fuer die Ampelspalte - GRUEN. Rot und Gelb teilen sich die Formate mit fehler/warnung. */
    final CellStyle ampelGruen;

    Formate(Workbook wb) {

        // Schriftarten gehoeren - wie Formate - zur Arbeitsmappe und werden einmal
        // erzeugt. Ein Font ist kein eigenstaendiges Objekt, sondern ein Eintrag in
        // der Schriftentabelle der Datei.
        Font fettGross = wb.createFont();
        fettGross.setBold(true);
        fettGross.setFontHeightInPoints((short) 14);

        Font fett = wb.createFont();
        fett.setBold(true);

        titel = wb.createCellStyle();
        titel.setFont(fettGross);

        // Kopfzeilen: fett, grau hinterlegt, zentriert.
        // setFillPattern nicht vergessen - ohne das bleibt die Farbe unsichtbar,
        // weil Excel dann nicht weiss, WIE es fuellen soll.
        kopfzeile = wb.createCellStyle();
        kopfzeile.setFont(fett);
        kopfzeile.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        kopfzeile.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        kopfzeile.setAlignment(HorizontalAlignment.CENTER);

        beschriftung = wb.createCellStyle();
        beschriftung.setFont(fett);

        // Zahlenformate sind Excel-Formatstrings - dieselben, die man im Dialog
        // "Zellen formatieren / Benutzerdefiniert" eintippt.
        //   #  = Ziffer, wird bei fuehrender Null weggelassen
        //   0  = Ziffer, wird immer angezeigt
        //   ,  = Tausendertrenner
        //   Text in Anfuehrungszeichen wird woertlich angehaengt
        geld = wb.createCellStyle();
        geld.setDataFormat(wb.createDataFormat().getFormat("#,##0.00 \"EUR\""));

        // ACHTUNG: Das %-Format multipliziert den Wert automatisch mit 100.
        // Man uebergibt also 0.878 und Excel zeigt "87,8 %".
        prozent = wb.createCellStyle();
        prozent.setDataFormat(wb.createDataFormat().getFormat("0.0 %"));

        // ROSE statt RED: kraeftiges Rot macht schwarze Schrift unlesbar.
        // Die Farbe soll die Zeile markieren, nicht den Text verstecken.
        fehler = wb.createCellStyle();
        fehler.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        fehler.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        warnung = wb.createCellStyle();
        warnung.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        warnung.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        ampelGruen = wb.createCellStyle();
        ampelGruen.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        ampelGruen.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }

    /**
     * Das Format zu einer Ampelstufe.
     *
     * <p>Die Zuordnung steht hier an EINER Stelle. Wuerde jeder Tab sie selbst
     * treffen, koennte Tab 3 rot anders faerben als Tab 4 - und niemand merkt es,
     * weil beide fuer sich plausibel aussehen.
     */
    CellStyle fuerAmpel(de.leo.controlling.abweichung.Ampel ampel) {
        return switch (ampel) {
            case GRUEN -> ampelGruen;
            case GELB -> warnung;
            case ROT -> fehler;
        };
    }
}
