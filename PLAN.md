# Controlling-Projekt — Implementierungsplan

**Ziel:** Ein Java-Programm, das aus `controlling_rohdaten.csv` (Plan/Ist je Produkt und Monat)
einen fertigen Excel-Monatsbericht erzeugt — so wie ihn ein Controller abliefern würde.

**Pipeline:** `CSV rein → Validierung → Deckungsbeitragsrechnung → Abweichungsanalyse → Excel raus`

> Stand: an die tatsächlich gelieferte CSV angepasst (Wide-Format, keine Plan/Ist-Trennung bei
> den variablen Stückkosten, zusätzliche Dimension `kostenstelle`, Gradle statt Maven).

---

## 0. Grundentscheidungen

| Thema | Entscheidung | Begründung |
|---|---|---|
| Java-Version | 25 (LTS, installiert) | `record`, Pattern Matching, `Stream.toList()` |
| Build | **Gradle 9.4.1** (Kotlin DSL) | ist installiert — Maven ist es nicht |
| Excel | Apache POI 5.x (`poi-ooxml`) | Standard für `.xlsx`; kommt erst in Phase 5 dazu |
| CSV | eigener Parser (~40 Zeilen) | Wir *wollen* kaputte Zeilen sehen, nicht wegnormalisiert bekommen |
| Tests | JUnit 5 | Jede Regel + jede Formel wird getestet |
| Geldbeträge | `BigDecimal` — **niemals `double`** | `0.1 + 0.2 != 0.3`; Controlling muss auf den Cent stimmen |
| Sprache im Code | Domäne deutsch (`Deckungsbeitrag`, `Abweichung`), Technik englisch (`CsvEinleser` → ok, `Config`) | Fachbegriffe bleiben 1:1 zum Bericht |
| Paket | `de.leo.controlling` | |

**Rundungsregel:** intern mit Scale 6 rechnen, erst bei der Ausgabe
`setScale(2, RoundingMode.HALF_UP)`. Sonst summieren sich Rundungsfehler und die
Abstimmbrücke geht nicht auf.

---

## 1. Rein — die Rohdaten-CSV

### 1.1 Tatsächliches Format

`controlling_rohdaten.csv` — **komma-separiert**, Punkt als Dezimaltrennzeichen (US-Format),
**Wide-Format** (Plan und Ist nebeneinander in einer Zeile):

```csv
monat,produkt,kostenstelle,plan_menge,ist_menge,plan_preis,ist_preis,variable_stueckkosten,fixkosten_produkt
2025-02,Produkt C,Vertrieb Nord,2000,2233.0,25.0,26.54,12.0,2000
```

| Spalte | Typ | Regel |
|---|---|---|
| `monat` | `YYYY-MM` | 2025-01 … 2025-12 |
| `produkt` | Text | Produkt A–D |
| `kostenstelle` | Text | `Vertrieb Nord` \| `Vertrieb Sued` |
| `plan_menge` | Ganzzahl ≥ 0 | |
| `ist_menge` | Dezimal ≥ 0 | |
| `plan_preis` | Dezimal > 0 | Nettoverkaufspreis |
| `ist_preis` | Dezimal > 0 | |
| `variable_stueckkosten` | Dezimal ≥ 0 | **gilt für Plan UND Ist** — keine Trennung! |
| `fixkosten_produkt` | Dezimal ≥ 0 | produktfixe Kosten des Monats |

### 1.2 Datenlage

- 49 Datenzeilen: 4 Produkte × 12 Monate = 48, plus 1 Dublette
- `variable_stueckkosten` und `fixkosten_produkt` sind je Produkt konstant
  (A: 30 / 5000 · B: 45 / 3000 · C: 12 / 2000 · D: 70 / 4000)
- `kostenstelle` wechselt pro Zeile — sie ist ein Attribut der Zeile, keine Produkteigenschaft

### 1.3 Die eingebauten Fehler (Erwartungswerte für die Tests)

| CSV-Zeile | Inhalt | Erwarteter Befund |
|---|---|---|
| 14 | `ist_preis = -77.05` | negativer Preis → FEHLER |
| 22 | `ist_menge` leer | Pflichtfeld fehlt → FEHLER |
| 30 | `ist_menge` leer | Pflichtfeld fehlt → FEHLER |
| 32 | `ist_menge` leer | Pflichtfeld fehlt → FEHLER |
| 46 | `ist_menge = 17950` bei Plan 350 | Ausreißer → WARNUNG |
| 6 + 21 | identische Zeile (2025-04, Produkt A, Nord) | Dublette → FEHLER |

### 1.4 Die Fixkosten-Falle

`fixkosten_produkt` steht auf **jeder** Zeile. Produkt A hat wegen der Dublette 13 Zeilen ×
5000 = 65.000 statt 60.000. Fixkosten sind ein **Attribut** der Produkt-Monats-Kombination,
kein addierbarer Messwert. Wer sie zeilenweise aufsummiert, rechnet falsch — und zwar
plausibel falsch, was schlimmer ist. Deshalb: Dubletten müssen vor der Aggregation raus,
und Fixkosten werden je (Produkt, Monat) **einmal** gezählt.

---

## 2. Verarbeitung, Modul 1 — Validierung

**Auftrag:** Fehlerhafte Zeilen erkennen und markieren, *bevor* sie die Analyse verfälschen.

### 2.1 Zwei Schweregrade — die zentrale Design-Entscheidung

| Schweregrad | Bedeutung | Konsequenz |
|---|---|---|
| `FEHLER` | Wert unbrauchbar | Zeile fliegt aus der Analyse, erscheint aber im Tab „Datenqualität" |
| `WARNUNG` | verdächtig, aber rechenbar | Zeile bleibt drin, wird im Report markiert |

Nichts wird stillschweigend verworfen. Jeder Befund trägt Zeilennummer, Feld, Regel-ID,
Originalwert und Klartextmeldung.

### 2.2 Regelkatalog

**Zeilen-Regeln** (brauchen nur die eine Zeile):

| ID | Regel | Grad |
|---|---|---|
| V01 | Spaltenanzahl ≠ 9 | FEHLER |
| V02 | Pflichtfeld leer | FEHLER |
| V03 | Zahl nicht parsebar | FEHLER |
| V04 | `monat` nicht `YYYY-MM` oder Monat außerhalb 01–12 | FEHLER |
| V05 | negative Menge / negativer Preis / negative Kosten | FEHLER |
| V06 | `variable_stueckkosten >= preis` → negativer Stück-DB | WARNUNG |
| V08 | Ausreißer: Ist-Menge oder Ist-Preis weicht > 50 % vom Plan ab | WARNUNG |

**Datensatz-Regeln** (brauchen alle Zeilen):

| ID | Regel | Grad |
|---|---|---|
| V07 | Dublette: `monat + produkt + kostenstelle` doppelt | FEHLER (beide Zeilen raus) |

Gegenüber der ersten Fassung entfallen die Regeln zu `szenario` und zu Plan/Ist-Waisen —
im Wide-Format kann es sie nicht geben. **V08 ist eine Zeilenregel**, keine Datensatzregel:
Plan und Ist stehen im Wide-Format nebeneinander in derselben Zeile, die Regel braucht die
übrigen Zeilen nicht.

### 2.3 Der Schweigegrundsatz

Auf Zeile 14 (`istPreis = -77.05`) könnten drei Regeln anspringen: V05 (negativ),
V06 (Kosten > Preis) und V08 (196 % Abweichung). Das wären drei Befunde für **ein**
Problem — der Bericht würde unlesbar, und es wäre unklar, was zu reparieren ist.

> **Jede Regel prüft genau ein Thema und schweigt, wenn ihre Eingabe für dieses Thema
> unbrauchbar ist.**

- Feld leer → nur V02 meldet; V03 und V05 schweigen.
- Feld nicht parsebar → nur V03 meldet; V05, V06, V08 schweigen.
- Feld negativ → V05 meldet; V06 und V08 rechnen damit nicht weiter.

Damit bekommt jede kaputte Stelle genau einen Befund — den, der die Ursache benennt.

### 2.4 Struktur

```java
public interface Zeilenregel    { List<Befund> pruefe(Rohzeile zeile); }
public interface Datensatzregel { List<Befund> pruefe(List<Rohzeile> alle); }

public record Befund(int zeilennummer, String feld, String regelId,
                     Schweregrad grad, String originalwert, String meldung) {}
```

Der `Validator` hält zwei Listen von Regeln und liefert ein `Pruefprotokoll` mit
`befunde()`, `verwertbareZeilen()` und `qualitaetsquote()`.
Neue Regel = neue Klasse + eine Zeile in der Registry. Kein `if`-Monster.

Die Regel-ID steht als Konstante *in* der Regelklasse, nicht im Klassennamen: IDs ändern
sich beim Umsortieren, der Name beschreibt dauerhaft, was geprüft wird.

### 2.5 Erwartete Befunde auf `controlling_rohdaten.csv`

Das ist das Akzeptanzkriterium der Phase — nicht mehr und nicht weniger:

| CSV-Zeile | Regel | Grad | Befund |
|---|---|---|---|
| 22 | V02 | FEHLER | `istMenge` ist leer |
| 30 | V02 | FEHLER | `istMenge` ist leer |
| 32 | V02 | FEHLER | `istMenge` ist leer |
| 14 | V05 | FEHLER | `istPreis` negativ (−77.05) |
| 6 | V07 | FEHLER | Dublette zu Zeile 21 |
| 21 | V07 | FEHLER | Dublette zu Zeile 6 |
| 46 | V08 | WARNUNG | `istMenge` 17950 weicht 5028 % von Plan 350 ab |

**7 Befunde · 6 Zeilen mit FEHLER · 43 verwertbare Zeilen · Qualitätsquote 87,8 %**

Zeile 46 bleibt in der Rechnung (nur Warnung) und verzerrt die Kennzahlen sichtbar — das
ist gewollt: Ein Controller soll den Ausreißer sehen, nicht eine stillschweigend geglättete
Zahl.

### 2.6 Definition of Done

- Genau die 7 Befunde aus 2.5 werden gefunden, mit der richtigen Regel-ID.
- Keine Falsch-Positiven auf den übrigen Zeilen.
- Ein Unit-Test pro Regel: ein gutes und ein schlechtes Beispiel.

---

## 3. Verarbeitung, Modul 2 — Deckungsbeitragsrechnung

### 3.1 Formeln

Je Produkt und Monat, getrennt für Plan und Ist — mit **denselben** variablen Stückkosten `k`:

```
Umsatz            = menge × preis
Variable Kosten   = menge × k
DB I              = Umsatz − Variable Kosten
DB I je Stück     = preis − k
DB-I-Marge        = DB I / Umsatz
DB II             = DB I − fixkosten_produkt
DB-II-Marge       = DB II / Umsatz
Break-Even-Menge  = fixkosten_produkt / DB I je Stück      (nur wenn Stück-DB > 0)
```

Auf Gesamtebene fürs Deckblatt: `Betriebsergebnis = Σ DB II`.
(Unternehmensfixkosten liefert die CSV nicht — optional aus der Konfiguration.)

### 3.2 Struktur

```java
public record Deckungsbeitrag(
    BigDecimal menge, BigDecimal umsatz, BigDecimal variableKosten,
    BigDecimal dbEins, BigDecimal fixkosten, BigDecimal dbZwei) {

    // Abgeleitete Kennzahlen sind METHODEN, keine Komponenten - so kann
    // niemand einen Deckungsbeitrag bauen, dessen Marge nicht zum DB I passt.
    BigDecimal dbEinsJeStueck();   // null wenn Menge = 0
    BigDecimal dbEinsMarge();      // null wenn Umsatz = 0
    BigDecimal dbZweiMarge();
    BigDecimal breakEvenMenge();   // null wenn Stueck-DB <= 0; sonst aufgerundet
}
```

Reine Funktionen, keine I/O, kein Zustand — dadurch trivial testbar.

### 3.3 Definition of Done

- Ein von Hand durchgerechnetes Produkt stimmt auf den Cent.
- Umsatz = 0 → Marge liefert `null`/„n/a", nicht `ArithmeticException`.
- Fixkosten je (Produkt, Monat) genau einmal gezählt (siehe 1.4).

---

## 4. Verarbeitung, Modul 3 — Abweichungsanalyse

### 4.1 Vorzeichenkonvention — vorher festlegen!

**Alle Abweichungen werden ergebnisorientiert dargestellt: „+" = ergebnisverbessernd.**
Das ist die häufigste Fehlerquelle in solchen Rechnungen — deshalb steht es hier und in einem
JavaDoc am `AbweichungsRechner`.

### 4.2 Formeln (Drei-Faktoren-Zerlegung)

Mit Plan `(m_p, p_p)` und Ist `(m_i, p_i)`, variable Stückkosten `k` für beide Seiten gleich:

**Umsatzabweichung**

```
Preisabweichung   = (p_i − p_p) × m_p
Mengenabweichung  = (m_i − m_p) × p_p
Mischabweichung   = (p_i − p_p) × (m_i − m_p)
────────────────────────────────────────────
Summe             = Umsatz_ist − Umsatz_plan      ← muss exakt aufgehen
```

**DB-I-Abweichung** — weil `k` konstant ist, wirkt die Menge mit dem Plan-Stück-DB:

```
Preisabweichung   = (p_i − p_p) × m_p
Mengenabweichung  = (m_i − m_p) × (p_p − k)
Mischabweichung   = (p_i − p_p) × (m_i − m_p)
────────────────────────────────────────────
Summe             = DB I_ist − DB I_plan
```

Eine **Kostenabweichung gibt es nicht** — die CSV liefert nur einen Wert für
`variable_stueckkosten`, keinen Plan/Ist-Split. Das ist keine Lücke im Programm, sondern
eine Eigenschaft der Datenbasis; sie gehört so in den Bericht geschrieben.

**DB-II-Abweichung** = DB-I-Abweichung + `Fixkostenabweichung = −(F_i − F_p)`.
Da die Fixkosten in dieser CSV Plan = Ist sind, ist dieser Term hier immer 0 — die Formel
bleibt trotzdem drin, damit der Bericht auch mit anderen Daten stimmt.

### 4.3 Abstimmbrücke — der eingebaute Selbsttest

```
Σ (Preis + Menge + Misch + Fix) über alle Produkte  ==  DB II_ist − DB II_plan
```

Das ist gleichzeitig ein JUnit-Assert **und** ein Excel-Tab (Wasserfall von Plan-DB II zu
Ist-DB II). Geht die Brücke nicht auf, schreibt der Report das sichtbar aufs Deckblatt,
statt es zu verschlucken.

### 4.4 Wesentlichkeitsgrenze

Ampel je Produkt, konfigurierbar (Default: 500 € **und** 5 %):
🟢 unter beiden Schwellen · 🟡 eine überschritten · 🔴 beide überschritten und negativ

### 4.5 Definition of Done

- Handrechnung eines Produkts stimmt.
- Abstimmbrücke geht für den kompletten Datensatz auf (Toleranz 0,01 €).
- Randfälle `m_p = 0` und `m_i = 0` laufen durch.

---

## 5. Raus — der Excel-Monatsbericht

### 5.1 Architektur-Regel

POI bleibt am Rand. Die Module liefern ein reines Datenobjekt `Berichtsmodell`;
`ExcelReportWriter` malt es nur noch. Dadurch ist der Bericht ohne Excel testbar.

### 5.2 Tabs

| # | Tab | Inhalt |
|---|---|---|
| 1 | **Deckblatt** | Berichtszeitraum, Erstellzeitpunkt, Datenquelle, Datenqualitätsquote, Betriebsergebnis Plan/Ist, Top-3-Abweichungen im Klartext, Warnung falls Brücke nicht aufgeht |
| 2 | **Datenqualität** | Alle Befunde: Zeile, Feld, Regel-ID, Grad, Originalwert, Meldung. Rot = FEHLER, gelb = WARNUNG |
| 3 | **DB-Rechnung** | Je Produkt: Menge, Umsatz, var. Kosten, DB I, DB-I-%, Fixkosten, DB II, DB-II-% — Plan / Ist / Δ. Summenzeile fett |
| 4 | **Abweichungsanalyse** | Je Produkt: Preis-, Mengen-, Mischabweichung, Gesamt, Ampel |
| 5 | **Abweichungsbrücke** | Plan-DB II → Einzeleffekte → Ist-DB II (Wasserfall + Balkendiagramm) |
| 6 | **Zeitreihe** | DB II je Produkt über 12 Monate, Plan vs. Ist, Liniendiagramm |
| 7 | **Kostenstellen** | DB II je Kostenstelle (Nord/Süd) — die zweite Sicht auf dieselben Daten |
| 8 | **Rohdaten** | Eingelesene Daten mit Statusspalte OK / WARN / FEHLER — für die Nachvollziehbarkeit |

### 5.3 Formatierung

- Zahlenformate `#.##0,00 €`, Prozent `0,0 %`, Mengen `#.##0`
- Kopfzeile fett + Hintergrund, `createFreezePane(1, 1)`
- Negative Abweichungen rot, positive grün
- `autoSizeColumn`, Dateiname `Monatsbericht_2025.xlsx`

---

## 6. Projektstruktur

```
Controlling-Projekt/
├─ build.gradle.kts
├─ settings.gradle.kts
├─ controlling_rohdaten.csv
├─ out/                                  (gitignored)
└─ src/
   ├─ main/java/de/leo/controlling/
   │  ├─ App.java                        CLI-Einstieg, verdrahtet die Pipeline
   │  ├─ config/Berichtskonfiguration.java
   │  ├─ io/CsvEinleser.java
   │  ├─ io/ExcelReportWriter.java
   │  ├─ model/  Rohzeile, Datenzeile, Produktmonat
   │  ├─ pruefung/  Validator, Befund, Schweregrad, Pruefprotokoll, regeln/V01…V08
   │  ├─ rechnung/  DeckungsbeitragsRechner, Deckungsbeitrag
   │  ├─ abweichung/ AbweichungsRechner, Abweichung, Ampel
   │  └─ report/    Berichtsmodell, BerichtsmodellBauer
   └─ test/java/de/leo/controlling/
```

### CLI (Endzustand)

```bash
gradle run --args="--input controlling_rohdaten.csv --output out/Monatsbericht_2025.xlsx"
```

Exit-Codes: `0` sauber · `1` mit Warnungen · `2` Fehlerzeilen vorhanden · `3` Abbruch.

---

## 7. Reihenfolge & Aufwand

| Phase | Inhalt | Aufwand | Status |
|---|---|---|---|
| **P0** | Gradle-Setup, `.gitignore`, Projektgerüst | 0,5 h | ✅ erledigt |
| **P1** | `Rohzeile` + `CsvEinleser` + `App` — Datei einlesen, roh | 2 h | ← **hier** |
| **P2** | Validierung: Interfaces, 8 Regeln, `Pruefprotokoll` + Tests | 4 h | |
| **P3** | `Datenzeile` (geparst) + Deckungsbeitragsrechnung + Tests | 3 h | |
| **P4** | Abweichungsanalyse + Abstimmbrücke + Tests | 4 h | |
| **P5** | `Berichtsmodell` + Excel-Writer, Tabs 1–4 | 5 h | |
| **P6** | Tabs 5–8, Diagramme, Formatierung | 3 h | |
| **P7** | CLI, Exit-Codes, README, Aufräumen | 2 h | |

**Gesamt ≈ 24 h.** Nach P4 ist die Fachlogik komplett — ab da wird es Kosmetik, und es gibt
jederzeit ein lauffähiges Zwischenergebnis.

**Reihenfolge-Regel:** Jede Phase endet mit grünen Tests und einem Commit. Kein Modul wird
angefangen, bevor das vorherige testgedeckt ist — sonst debuggt man am Ende die
Abweichungsanalyse, obwohl der CSV-Parser das Komma falsch liest.

---

## 8. Bewusst nicht im Scope

Datenbank statt CSV · mehrstufige Fixkostendeckung über Produktgruppen (DB III/IV) ·
Forecast/Hochrechnung · PDF-Ausgabe · Web-Frontend · Mehrwährungsfähigkeit
