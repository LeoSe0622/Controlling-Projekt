package de.leo.controlling.report;

import de.leo.controlling.abweichung.Abweichung;
import de.leo.controlling.pruefung.Befund;
import de.leo.controlling.pruefung.Schweregrad;
import de.leo.controlling.model.Rohzeile;
import de.leo.controlling.rechnung.Kostenstellenergebnis;
import de.leo.controlling.rechnung.Monatsergebnis;
import de.leo.controlling.rechnung.Produktergebnis;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Schreibt ein {@link Berichtsmodell} als Excel-Datei.
 *
 * <p>Wie der {@link KonsolenReport} rechnet diese Klasse nichts — sie liest dieselben
 * Getter und trifft nur andere Darstellungsentscheidungen. Aus {@code [!!]} wird hier
 * eine rote Zelle.
 *
 * <p>Aufbau einer xlsx bei POI, von aussen nach innen:
 * <pre>
 *   Workbook  (die Datei)
 *     Sheet   (ein Tabellenblatt)
 *       Row   (eine Zeile - muss erzeugt werden, bevor man sie benutzt)
 *         Cell (eine Zelle - ebenfalls)
 * </pre>
 * Es gibt kein "schreib mal in Zeile 5": Existiert Zeile 5 noch nicht, liefert
 * {@code getRow(5)} ein {@code null}. Deshalb immer {@code createRow}.
 */
public final class ExcelReportWriter {

    private static final DateTimeFormatter ZEITSTEMPEL =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * @param m    das fertige Berichtsmodell
     * @param ziel Pfad der zu schreibenden .xlsx-Datei
     */
    public void schreibe(Berichtsmodell m, Path ziel) throws IOException {

        try (XSSFWorkbook wb = new XSSFWorkbook();
             OutputStream out = Files.newOutputStream(ziel)) {

            Formate f = new Formate(wb);

            deckblatt(wb, f, m);
            datenqualitaet(wb, f, m);
            deckungsbeitraege(wb, f, m);
            abweichungen(wb, f, m);
            abweichungsbruecke(wb, f, m);
            zeitreihe(wb, f, m);
            kostenstellen(wb, f, m);
            rohdaten(wb, f, m);

            wb.write(out);
        }
    }

    /** Tab 1: Kennzahlen auf einen Blick. */
    private void deckblatt(XSSFWorkbook wb, Formate f, Berichtsmodell m) {
        Sheet blatt = wb.createSheet("Deckblatt");

        text(blatt, 0, 0, "Controlling-Monatsbericht", f.titel);

        text(blatt, 2, 0, "Datenquelle", f.beschriftung);
        text(blatt, 2, 1, m.quelle(), null);
        text(blatt, 3, 0, "Erstellt am", f.beschriftung);
        text(blatt, 3, 1, m.erstelltAm().format(ZEITSTEMPEL), null);
        text(blatt, 4, 0, "Zeitraum", f.beschriftung);
        text(blatt, 4, 1, m.erwarteteMonate() + " Monate", null);

        text(blatt, 6, 0, "Datenqualitaet", f.kopfzeile);
        text(blatt, 6, 1, "", f.kopfzeile);

        text(blatt, 7, 0, "Eingelesene Zeilen", f.beschriftung);
        zahl(blatt, 7, 1, BigDecimal.valueOf(m.alleZeilen().size()), null);

        text(blatt, 8, 0, "Verwertbare Zeilen", f.beschriftung);
        zahl(blatt, 8, 1, BigDecimal.valueOf(m.protokoll().verwertbareZeilen().size()), null);

        text(blatt, 9, 0, "Befunde gesamt", f.beschriftung);
        zahl(blatt, 9, 1, BigDecimal.valueOf(m.protokoll().befunde().size()), null);

        text(blatt, 10, 0, "davon Fehler", f.beschriftung);
        zahl(blatt, 10, 1, BigDecimal.valueOf(m.protokoll().anzahlFehler()), null);

        text(blatt, 11, 0, "davon Warnungen", f.beschriftung);
        zahl(blatt, 11, 1, BigDecimal.valueOf(m.protokoll().anzahlWarnungen()), null);

        text(blatt, 12, 0, "Qualitaetsquote", f.beschriftung);
        zahl(blatt, 12, 1, BigDecimal.valueOf(m.protokoll().qualitaetsquote()), f.prozent);

        text(blatt, 14, 0, "Betriebsergebnis", f.kopfzeile);
        text(blatt, 14, 1, "", f.kopfzeile);

        text(blatt, 15, 0, "Plan", f.beschriftung);
        zahl(blatt, 15, 1, m.gesamtPlan().dbZwei(), f.geld);

        text(blatt, 16, 0, "Ist", f.beschriftung);
        zahl(blatt, 16, 1, m.gesamtIst().dbZwei(), f.geld);

        text(blatt, 17, 0, "Abweichung", f.beschriftung);
        zahl(blatt, 17, 1,
                m.gesamtIst().dbZwei().subtract(m.gesamtPlan().dbZwei()), f.geld);

        if (m.brueckeGehtAuf()) {
            text(blatt, 19, 0, "Abstimmbruecke: geht auf.", null);
        } else {
            text(blatt, 19, 0,
                    "ACHTUNG - Abstimmbruecke geht NICHT auf. Differenz: "
                            + m.brueckenDifferenz(),
                    f.fehler);
        }

        blatt.autoSizeColumn(0);
        blatt.autoSizeColumn(1);
    }

    /** Tab 2: alle Befunde der Validierung. */
    private void datenqualitaet(XSSFWorkbook wb, Formate f, Berichtsmodell m) {
        Sheet blatt = wb.createSheet("Datenqualitaet");

        String[] spalten = {"Zeile", "Feld", "Regel", "Grad", "Originalwert", "Meldung"};
        for (int i = 0; i < spalten.length; i++) {
            text(blatt, 0, i, spalten[i], f.kopfzeile);
        }

        int zeile = 1;
        for (Befund b : m.protokoll().befunde()) {

            CellStyle stil = b.grad() == Schweregrad.FEHLER ? f.fehler : f.warnung;

            zahl(blatt, zeile, 0, BigDecimal.valueOf(b.zeilennummer()), stil);
            text(blatt, zeile, 1, b.feld(), stil);
            text(blatt, zeile, 2, b.regelId(), stil);
            text(blatt, zeile, 3, b.grad().name(), stil);
            text(blatt, zeile, 4, b.originalwert(), stil);
            text(blatt, zeile, 5, b.meldung(), stil);
            zeile++;
        }

        blatt.createFreezePane(0, 1);

        for (int i = 0; i < spalten.length; i++) {
            blatt.autoSizeColumn(i);
        }
    }

    /**
    /**
     * Tab 3: Deckungsbeitragsrechnung je Produkt.
     *
     * <p>Eine ZEILE je Produkt, eine SPALTE je Kennzahl - eine flache Tabelle.
     * Das klassische Deckungsbeitragsschema (Kennzahlen als Zeilen) liest sich
     * schoener, laesst sich in Excel aber weder sortieren noch filtern noch in
     * eine Pivot-Tabelle verwandeln. Hier gewinnt Brauchbarkeit vor Optik.
     */
    private void deckungsbeitraege(XSSFWorkbook wb, Formate f, Berichtsmodell m) {
        Sheet blatt = wb.createSheet("DB-Rechnung");

        String[] spalten = {
                "Produkt", "Monate",
                "Menge Plan", "Menge Ist",
                "Umsatz Plan", "Umsatz Ist",
                "Var. Kosten Plan", "Var. Kosten Ist",
                "DB I Plan", "DB I Ist",
                "DB-I-Marge Plan", "DB-I-Marge Ist",
                "Fixkosten",
                "DB II Plan", "DB II Ist", "Abweichung",
                "Break-Even Ist"
        };
        for (int i = 0; i < spalten.length; i++) {
            text(blatt, 0, i, spalten[i], f.kopfzeile);
        }

        int zeile = 1;
        for (Produktergebnis e : m.produkte()) {
            text(blatt, zeile, 0, e.produkt(), null);
            text(blatt, zeile, 1, e.monate() + "/" + m.erwarteteMonate(), null);

            zahl(blatt, zeile, 2, e.plan().menge(), null);
            zahl(blatt, zeile, 3, e.ist().menge(), null);

            zahl(blatt, zeile, 4, e.plan().umsatz(), f.geld);
            zahl(blatt, zeile, 5, e.ist().umsatz(), f.geld);
            zahl(blatt, zeile, 6, e.plan().variableKosten(), f.geld);
            zahl(blatt, zeile, 7, e.ist().variableKosten(), f.geld);
            zahl(blatt, zeile, 8, e.plan().dbEins(), f.geld);
            zahl(blatt, zeile, 9, e.ist().dbEins(), f.geld);

            zahlWennVorhanden(blatt, zeile, 10, e.plan().dbEinsMarge(), f.prozent);
            zahlWennVorhanden(blatt, zeile, 11, e.ist().dbEinsMarge(), f.prozent);

            zahl(blatt, zeile, 12, e.plan().fixkosten(), f.geld);
            zahl(blatt, zeile, 13, e.plan().dbZwei(), f.geld);
            zahl(blatt, zeile, 14, e.ist().dbZwei(), f.geld);
            zahl(blatt, zeile, 15, e.dbZweiAbweichung(), f.geld);

            zahlWennVorhanden(blatt, zeile, 16, e.ist().breakEvenMenge(), null);
            zeile++;
        }

        text(blatt, zeile, 0, "GESAMT", f.beschriftung);
        zahl(blatt, zeile, 4, m.gesamtPlan().umsatz(), f.geld);
        zahl(blatt, zeile, 5, m.gesamtIst().umsatz(), f.geld);
        zahl(blatt, zeile, 6, m.gesamtPlan().variableKosten(), f.geld);
        zahl(blatt, zeile, 7, m.gesamtIst().variableKosten(), f.geld);
        zahl(blatt, zeile, 8, m.gesamtPlan().dbEins(), f.geld);
        zahl(blatt, zeile, 9, m.gesamtIst().dbEins(), f.geld);
        zahlWennVorhanden(blatt, zeile, 10, m.gesamtPlan().dbEinsMarge(), f.prozent);
        zahlWennVorhanden(blatt, zeile, 11, m.gesamtIst().dbEinsMarge(), f.prozent);
        zahl(blatt, zeile, 12, m.gesamtPlan().fixkosten(), f.geld);
        zahl(blatt, zeile, 13, m.gesamtPlan().dbZwei(), f.geld);
        zahl(blatt, zeile, 14, m.gesamtIst().dbZwei(), f.geld);
        zahl(blatt, zeile, 15,
                m.gesamtIst().dbZwei().subtract(m.gesamtPlan().dbZwei()), f.geld);

        blatt.createFreezePane(1, 1);
        for (int i = 0; i < spalten.length; i++) {
            blatt.autoSizeColumn(i);
        }
    }

    /** Tab 4: Zerlegung der Abweichung je Produkt, mit farbiger Ampel. */
    private void abweichungen(XSSFWorkbook wb, Formate f, Berichtsmodell m) {
        Sheet blatt = wb.createSheet("Abweichungsanalyse");

        String[] spalten = {
                "Produkt", "Preisabweichung", "Mengenabweichung",
                "Mischabweichung", "Fixkostenabweichung", "Gesamt", "Ampel"
        };
        for (int i = 0; i < spalten.length; i++) {
            text(blatt, 0, i, spalten[i], f.kopfzeile);
        }

        int zeile = 1;
        for (Produktergebnis e : m.produkte()) {
            Abweichung a = m.abweichungen().get(e.produkt());

            text(blatt, zeile, 0, e.produkt(), null);
            zahl(blatt, zeile, 1, a.preisabweichung(), f.geld);
            zahl(blatt, zeile, 2, a.mengenabweichung(), f.geld);
            zahl(blatt, zeile, 3, a.mischabweichung(), f.geld);
            zahl(blatt, zeile, 4, a.fixkostenabweichung(), f.geld);
            zahl(blatt, zeile, 5, a.gesamt(), f.geld);

            text(blatt, zeile, 6, "", f.fuerAmpel(m.ampelFuer(e)));
            zeile++;
        }

        Abweichung gesamt = m.gesamtAbweichung();
        text(blatt, zeile, 0, "GESAMT", f.beschriftung);
        zahl(blatt, zeile, 1, gesamt.preisabweichung(), f.geld);
        zahl(blatt, zeile, 2, gesamt.mengenabweichung(), f.geld);
        zahl(blatt, zeile, 3, gesamt.mischabweichung(), f.geld);
        zahl(blatt, zeile, 4, gesamt.fixkostenabweichung(), f.geld);
        zahl(blatt, zeile, 5, gesamt.gesamt(), f.geld);

        int brueckenZeile = zeile + 2;
        if (m.brueckeGehtAuf()) {
            text(blatt, brueckenZeile, 0, "Abstimmbruecke: geht auf.", null);
        } else {
            text(blatt, brueckenZeile, 0,
                    "ACHTUNG - Abstimmbruecke geht NICHT auf. Differenz: "
                            + m.brueckenDifferenz(),
                    f.fehler);
        }

        blatt.createFreezePane(1, 1);
        for (int i = 0; i < spalten.length; i++) {
            blatt.autoSizeColumn(i);
        }
    }

    /**
     * Tab 5: Abweichungsbruecke - der Weg vom Plan-DB-II zum Ist-DB-II.
     *
     * <p>Die Spalte "Kumuliert" macht daraus eine Bruecke: Man sieht den Saldo Schritt
     * fuer Schritt wandern. Die letzte Zeile MUSS dem Ist-DB-II entsprechen - wenn nicht,
     * fehlt ein Effekt in der Zerlegung.
     */
    private void abweichungsbruecke(XSSFWorkbook wb, Formate f, Berichtsmodell m) {
        Sheet blatt = wb.createSheet("Abweichungsbruecke");

        String[] spalten = {"Schritt", "Betrag", "Kumuliert"};
        for (int i = 0; i < spalten.length; i++) {
            text(blatt, 0, i, spalten[i], f.kopfzeile);
        }

        Abweichung a = m.gesamtAbweichung();

        BigDecimal saldo = m.gesamtPlan().dbZwei();

        text(blatt, 1, 0, "Plan-DB II", f.beschriftung);
        zahl(blatt, 1, 2, saldo, f.geld);

        saldo = saldo.add(a.preisabweichung());
        text(blatt, 2, 0, "Preisabweichung", null);
        zahl(blatt, 2, 1, a.preisabweichung(), f.geld);
        zahl(blatt, 2, 2, saldo, f.geld);

        saldo = saldo.add(a.mengenabweichung());
        text(blatt, 3, 0, "Mengenabweichung", null);
        zahl(blatt, 3, 1, a.mengenabweichung(), f.geld);
        zahl(blatt, 3, 2, saldo, f.geld);

        saldo = saldo.add(a.mischabweichung());
        text(blatt, 4, 0, "Mischabweichung", null);
        zahl(blatt, 4, 1, a.mischabweichung(), f.geld);
        zahl(blatt, 4, 2, saldo, f.geld);

        saldo = saldo.add(a.fixkostenabweichung());
        text(blatt, 5, 0, "Fixkostenabweichung", null);
        zahl(blatt, 5, 1, a.fixkostenabweichung(), f.geld);
        zahl(blatt, 5, 2, saldo, f.geld);

        text(blatt, 6, 0, "Ist-DB II", f.beschriftung);
        zahl(blatt, 6, 2, m.gesamtIst().dbZwei(), f.geld);

        if (saldo.compareTo(m.gesamtIst().dbZwei()) == 0) {
            text(blatt, 8, 0, "Bruecke geht auf: Der Saldo trifft den Ist-DB-II exakt.", null);
        } else {
            text(blatt, 8, 0,
                    "ACHTUNG - Bruecke geht NICHT auf. Endsaldo " + saldo
                            + " vs. Ist-DB II " + m.gesamtIst().dbZwei(),
                    f.fehler);
        }

        for (int i = 0; i < spalten.length; i++) {
            blatt.autoSizeColumn(i);
        }
    }

    /**
     * Tab 6: Zeitreihe - DB II je Produkt ueber die Monate.
     *
     * <p><b>Andere Anordnung als in Tab 3, mit Absicht:</b> Monate als Zeilen, Produkte
     * als Spalten. Tab 3 ist flach, damit Excel sortieren und pivotieren kann. Diesen Tab
     * soll man MARKIEREN und daraus mit zwei Klicks ein Liniendiagramm machen koennen -
     * dafuer braucht es das breite Format.
     *
     * <p>Das Layout folgt dem Zweck, nicht einer Regel.
     */
    private void zeitreihe(XSSFWorkbook wb, Formate f, Berichtsmodell m) {
        Sheet blatt = wb.createSheet("Zeitreihe");

        List<Produktergebnis> produkte = m.produkte();

        text(blatt, 0, 0, "Monat", f.kopfzeile);
        for (int p = 0; p < produkte.size(); p++) {
            text(blatt, 0, 1 + p * 2, produkte.get(p).produkt() + " Plan", f.kopfzeile);
            text(blatt, 0, 2 + p * 2, produkte.get(p).produkt() + " Ist", f.kopfzeile);
        }

        List<YearMonth> monate = m.zeitreihe().stream()
                .map(Monatsergebnis::monat)
                .distinct()
                .sorted()
                .toList();

        for (int z = 0; z < monate.size(); z++) {
            YearMonth monat = monate.get(z);
            text(blatt, z + 1, 0, monat.toString(), null);

            for (int p = 0; p < produkte.size(); p++) {
                String produkt = produkte.get(p).produkt();

                Monatsergebnis treffer = m.zeitreihe().stream()
                        .filter(e -> e.produkt().equals(produkt) && e.monat().equals(monat))
                        .findFirst()
                        .orElse(null);

                if (treffer != null) {
                    zahl(blatt, z + 1, 1 + p * 2, treffer.plan().dbZwei(), f.geld);
                    zahl(blatt, z + 1, 2 + p * 2, treffer.ist().dbZwei(), f.geld);
                }
            }
        }

        blatt.createFreezePane(1, 1);
        for (int i = 0; i <= produkte.size() * 2; i++) {
            blatt.autoSizeColumn(i);
        }
    }

    /** Tab 7: Ergebnis je Kostenstelle - die zweite Sicht auf dieselben Daten. */
    private void kostenstellen(XSSFWorkbook wb, Formate f, Berichtsmodell m) {
        Sheet blatt = wb.createSheet("Kostenstellen");

        String[] spalten = {
                "Kostenstelle", "Zeilen",
                "Umsatz Plan", "Umsatz Ist",
                "DB I Plan", "DB I Ist",
                "DB II Plan", "DB II Ist"
        };
        for (int i = 0; i < spalten.length; i++) {
            text(blatt, 0, i, spalten[i], f.kopfzeile);
        }

        int zeile = 1;
        for (Kostenstellenergebnis k : m.kostenstellen()) {
            text(blatt, zeile, 0, k.kostenstelle(), null);
            zahl(blatt, zeile, 1, BigDecimal.valueOf(k.zeilen()), null);
            zahl(blatt, zeile, 2, k.plan().umsatz(), f.geld);
            zahl(blatt, zeile, 3, k.ist().umsatz(), f.geld);
            zahl(blatt, zeile, 4, k.plan().dbEins(), f.geld);
            zahl(blatt, zeile, 5, k.ist().dbEins(), f.geld);
            zahl(blatt, zeile, 6, k.plan().dbZwei(), f.geld);
            zahl(blatt, zeile, 7, k.ist().dbZwei(), f.geld);
            zeile++;
        }

        text(blatt, zeile + 1, 0,
                "Hinweis: Der DB II enthaelt produktfixe Kosten. Die gehoeren fachlich zum "
                        + "Produkt, nicht zur Region, und werden hier derjenigen Kostenstelle "
                        + "zugerechnet, die im jeweiligen Monat auf der Zeile stand. "
                        + "Aussagekraeftig ist vor allem der DB I.",
                null);

        for (int i = 0; i < spalten.length; i++) {
            blatt.autoSizeColumn(i);
        }
    }

    /**
     * Tab 8: alle eingelesenen Zeilen mit Statusspalte - fuer die Nachvollziehbarkeit.
     *
     * <p>Hier steht, was TATSAECHLICH in der Datei stand: alle 49 Zeilen, auch die
     * aussortierten. Wer eine Zahl im Bericht anzweifelt, findet hier die Quelle.
     */
    private void rohdaten(XSSFWorkbook wb, Formate f, Berichtsmodell m) {
        Sheet blatt = wb.createSheet("Rohdaten");

        String[] spalten = {
                "Zeile", "Status", "monat", "produkt", "kostenstelle",
                "plan_menge", "ist_menge", "plan_preis", "ist_preis",
                "variable_stueckkosten", "fixkosten_produkt"
        };
        for (int i = 0; i < spalten.length; i++) {
            text(blatt, 0, i, spalten[i], f.kopfzeile);
        }

        int zeile = 1;
        for (Rohzeile z : m.alleZeilen()) {
            Schweregrad status = m.statusVon(z.zeilennummer());

            String statusText;
            CellStyle stil;
            if (status == Schweregrad.FEHLER) {
                statusText = "FEHLER";
                stil = f.fehler;
            } else if (status == Schweregrad.WARNUNG) {
                statusText = "WARNUNG";
                stil = f.warnung;
            } else {
                statusText = "OK";
                stil = null;
            }

            zahl(blatt, zeile, 0, BigDecimal.valueOf(z.zeilennummer()), stil);
            text(blatt, zeile, 1, statusText, stil);

            text(blatt, zeile, 2, z.monat(), stil);
            text(blatt, zeile, 3, z.produkt(), stil);
            text(blatt, zeile, 4, z.kostenstelle(), stil);
            text(blatt, zeile, 5, z.planMenge(), stil);
            text(blatt, zeile, 6, z.istMenge(), stil);
            text(blatt, zeile, 7, z.planPreis(), stil);
            text(blatt, zeile, 8, z.istPreis(), stil);
            text(blatt, zeile, 9, z.variableStueckkosten(), stil);
            text(blatt, zeile, 10, z.fixkostenProdukt(), stil);
            zeile++;
        }

        blatt.createFreezePane(2, 1);
        for (int i = 0; i < spalten.length; i++) {
            blatt.autoSizeColumn(i);
        }
    }

    /** Schreibt Text in eine Zelle und legt Zeile/Zelle bei Bedarf an. */
    private static void text(Sheet blatt, int zeile, int spalte, String wert, CellStyle stil) {
        Cell zelle = zelle(blatt, zeile, spalte);
        zelle.setCellValue(wert);
        if (stil != null) {
            zelle.setCellStyle(stil);
        }
    }

    /**
     * Schreibt eine Zahl.
     *
     * <p>{@code doubleValue()} ist hier unbedenklich: Excel speichert Zahlen ohnehin
     * als double, und unsere BigDecimal-Werte sind bereits auf zwei Stellen gerundet.
     * Gerechnet wurde exakt — der double entsteht erst im letzten Schritt, bei der
     * Darstellung.
     */
    private static void zahl(Sheet blatt, int zeile, int spalte, BigDecimal wert, CellStyle stil) {
        Cell zelle = zelle(blatt, zeile, spalte);
        zelle.setCellValue(wert.doubleValue());
        if (stil != null) {
            zelle.setCellStyle(stil);
        }
    }

    /**
     * Schreibt eine Zahl nur, wenn es sie gibt - sonst bleibt die Zelle leer.
     *
     * <p>Margen und Break-Even sind {@code null}, wenn keine Menge oder kein Umsatz
     * vorliegt. Eine 0 hinzuschreiben waere eine Falschaussage: Die Kennzahl ist
     * nicht null, sie ist NICHT DEFINIERT. Die leere Zelle sagt genau das.
     */
    private static void zahlWennVorhanden(Sheet blatt, int zeile, int spalte,
                                          BigDecimal wert, CellStyle stil) {
        if (wert != null) {
            zahl(blatt, zeile, spalte, wert, stil);
        }
    }

    /** Holt eine Zelle - und legt Zeile wie Zelle an, falls es sie noch nicht gibt. */
    private static Cell zelle(Sheet blatt, int zeile, int spalte) {
        Row r = blatt.getRow(zeile);
        if (r == null) {
            r = blatt.createRow(zeile);
        }
        Cell c = r.getCell(spalte);
        if (c == null) {
            c = r.createCell(spalte);
        }
        return c;
    }
}
