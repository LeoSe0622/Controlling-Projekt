# Controlling-Projekt

Erzeugt aus einer Rohdaten-CSV (Plan/Ist je Produkt und Monat) einen Excel-Monatsbericht —
mit Datenqualitätsprüfung, Deckungsbeitragsrechnung und Abweichungsanalyse.

```
CSV rein → Validierung → Deckungsbeitragsrechnung → Abweichungsanalyse → Excel raus
```

## Benutzen

```bash
# bauen (einmalig, und nach jeder Änderung)
./gradlew installDist

# aufrufen
build/install/controlling-report/bin/controlling-report daten/maerz.csv
```

Unter Windows entsprechend `controlling-report.bat`.

### Optionen

| Option | Bedeutung |
|---|---|
| `--input <pfad>` | die einzulesende CSV (Pflicht) |
| `--output <pfad>` | Zieldatei; ohne Angabe entsteht sie **neben der Eingabedatei** |
| `--schwelle-eur <betrag>` | Ampel-Schwelle in Euro (Vorgabe: 500) |
| `--schwelle-prozent <zahl>` | Ampel-Schwelle in Prozent (Vorgabe: 5) |
| `--help` | Hilfetext |

Der Eingabepfad darf auch **ohne** `--input` angegeben werden — das ist Vorbereitung für
Phase 8, wo eine CSV auf eine Batch-Datei gezogen wird und Windows den nackten Pfad übergibt.

### Exit-Codes

| Code | Bedeutung |
|---|---|
| 0 | sauber |
| 1 | nur Warnungen |
| 2 | Fehlerzeilen in den Daten |
| 3 | Abbruch — Argumente falsch oder Datei unlesbar |

Bei Code 3 existiert **kein** Bericht. Wer das Programm automatisiert aufruft, muss das
auswerten, bevor er die Ausgabedatei öffnet.

## Entwickeln

```bash
./gradlew build          # übersetzen + alle Tests
./gradlew test           # nur Tests
./gradlew run --args="controlling_rohdaten.csv"
```

`./gradlew` statt `gradle`: Der Wrapper legt die Gradle-Version fest, sodass das Projekt
ohne installiertes Gradle baut und auf jedem Rechner dieselbe Version benutzt.

**Bei Widersprüchen zwischen IDE und `./gradlew build` hat Gradle recht.** Zeigt der Editor
Fehler, die der Build nicht kennt, ist sein Klassenpfad veraltet — in VS Code hilft
`Java: Clean Java Language Server Workspace`.

## Aufbau

| Paket | Aufgabe |
|---|---|
| `config` | Kommandozeile einlesen, Vorgaben setzen |
| `io` | CSV lesen — verändert nichts, prüft nichts |
| `model` | `Rohzeile` (alles String) und `Datenzeile` (geprüft, `BigDecimal`) |
| `pruefung` | acht Regeln V01–V08, `Validator`, `Pruefprotokoll` |
| `rechnung` | Deckungsbeiträge, Aggregation je Produkt / Monat / Kostenstelle |
| `abweichung` | Preis-, Mengen-, Mischeffekt; Wesentlichkeit und Ampel |
| `report` | `Berichtsmodell` (was ist wahr) plus Konsolen- und Excel-Ausgabe |

Zwei Regeln, die den Aufbau tragen:

- **Geld ist `BigDecimal`, nie `double`.** Gerechnet wird exakt; `double` entsteht erst
  beim Schreiben in Excel, das intern nichts anderes kennt.
- **Das `Berichtsmodell` weiß, *was* wahr ist; die Writer entscheiden, *wie* es aussieht.**
  Deshalb liefern Konsole und Excel dieselben Zahlen, ohne dass die Logik doppelt existiert.

## Datenformat

Komma-separiert, Punkt als Dezimaltrennzeichen, neun Spalten:

```csv
monat,produkt,kostenstelle,plan_menge,ist_menge,plan_preis,ist_preis,variable_stueckkosten,fixkosten_produkt
2025-02,Produkt C,Vertrieb Nord,2000,2233.0,25.0,26.54,12.0,2000
```

Kaputte Zeilen führen nicht zum Abbruch: Sie werden im Tab „Datenqualität" gemeldet.
Zeilen mit **FEHLER** fliegen aus der Rechnung, Zeilen mit **WARNUNG** bleiben drin und
werden markiert — damit ein Ausreißer sichtbar bleibt, statt stillschweigend geglättet
zu werden.

Details zu den Regeln und den Formeln stehen in [PLAN.md](PLAN.md).
