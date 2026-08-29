# Entstehung

Das fachliche Konzept — Deckungsbeitragsstufen, Break-even, Abweichungslogik und der Aufbau des Berichts — stammt von mir und ist gegen eigene Rechenbeispiele geprüft. Planung und Implementierung entstanden KI-unterstützt.

# Controlling-Projekt

Erzeugt aus einer Rohdaten-CSV (Plan/Ist je Produkt und Monat) einen Excel-Monatsbericht —
mit Datenqualitätsprüfung, Deckungsbeitragsrechnung und Abweichungsanalyse.

```
CSV rein → Validierung → Deckungsbeitragsrechnung → Abweichungsanalyse → Excel raus
```

## Einmalig: bauen

```bash
gradlew installDist
```

Nötig nach jeder Code-Änderung. Für das reine Benutzen danach nie wieder.

## Im Alltag: CSV auf `Monatsbericht.bat` ziehen

Das ist alles. Es passiert:

1. Ein Konsolenfenster geht auf und zeigt die Befunde und beide Tabellen
2. Excel öffnet sich mit dem achtseitigen Bericht
3. Unten steht im Klartext, ob es Beanstandungen gab
4. Das Fenster bleibt offen, bis man eine Taste drückt

Der Bericht landet **neben der Eingabedatei**: aus `maerz.csv` wird `maerz_Monatsbericht.xlsx`.
Das ist wichtig, weil beim Reinziehen das Arbeitsverzeichnis irgendein Windows-Systemordner
ist — im Projektordner würde man ihn nicht wiederfinden.

## Auf der Kommandozeile

```bash
build/install/controlling-report/bin/controlling-report daten/maerz.csv
```

Unter Windows entsprechend `controlling-report.bat`.

### Optionen

| Option | Bedeutung |
|---|---|
| `--input <pfad>` | die einzulesende CSV (Pflicht) |
| `--output <pfad>` | Zieldatei; ohne Angabe entsteht sie **neben der Eingabedatei** |
| `--schwelle-eur <betrag>` | Ampel-Schwelle in Euro. **Ohne Angabe abgeleitet:** 0,25 % des Plan-Ergebnisses, mindestens 500 |
| `--schwelle-prozent <zahl>` | Ampel-Schwelle in Prozent (Vorgabe: 5) |
| `--kein-oeffnen` | Bericht **nicht** automatisch öffnen — für Skripte |
| `--help` | Hilfetext |

Der Eingabepfad darf auch **ohne** `--input` angegeben werden. Windows übergibt beim
Reinziehen den nackten Pfad, deshalb muss beides funktionieren.

`--kein-oeffnen` ist kein Beiwerk: Ein nächtlicher Job, der Excel aufmacht, blockiert bis
zum Morgen. Erst dieser Schalter macht die Exit-Codes unten praktisch nutzbar.

**Warum die Euro-Schwelle mitwächst:** Eine feste Vorgabe von 500 € war an einem Datensatz
mit 48 Zeilen kalibriert. Auf 1.810 Zeilen mit sechsstelligen Jahres-Deckungsbeiträgen
reißt *jede* Abweichung diese Schwelle — 13 von 15 Produkten wurden gelb, und die Ampel
sagte nichts mehr. Die Prozent-Schwelle skaliert von allein mit, die absolute nicht.
Wer eine feste Schwelle will, gibt sie an; dann gilt genau sie.

### Exit-Codes

| Code | Bedeutung |
|---|---|
| 0 | sauber |
| 1 | nur Warnungen |
| 2 | Fehlerzeilen in den Daten — **Bericht existiert trotzdem** |
| 3 | Abbruch: Argumente falsch, Eingabe unlesbar oder Ausgabedatei gesperrt |

Der häufigste Fall von Code 3 im Alltag: Die Zieldatei ist noch in Excel geöffnet. Das
Programm sagt das im Klartext.

Bei Code 3 existiert **kein** Bericht. Wer das Programm automatisiert aufruft, muss das
auswerten, bevor er die Ausgabedatei öffnet.

## Was herauskommt

Acht Tabs, jeder beantwortet eine Frage:

| Tab | Frage |
|---|---|
| **Deckblatt** | Kann ich diesen Zahlen trauen? |
| **Datenqualität** | Was genau war kaputt, und wo? |
| **DB-Rechnung** | Wie steht jedes Produkt da? |
| **Abweichungsanalyse** | Woran lag es — Preis oder Menge? |
| **Abweichungsbrücke** | Wie komme ich vom Plan zum Ist? |
| **Zeitreihe** | War es das ganze Jahr so oder ein einzelner Monat? |
| **Kostenstellen** | Wie stehen die Vertriebsregionen da? |
| **Rohdaten** | Was stand tatsächlich in der Datei? |

Auf dem Deckblatt steht rechts neben den Kennzahlen ein **Liniendiagramm: die Abweichung
gegen den Plan je Monat**, gegen eine Nulllinie. Oben heißt besser als geplant, unten
schlechter. Eine Linie statt zwei — Plan und Ist nebeneinander ergäben zwei fast
deckungsgleiche Kurven, und die interessante Größe ist ihr Abstand.

Es ist ein echtes Excel-Diagramm, kein Bild: anklickbar, umformatierbar, erweiterbar. Seine
Daten liest es aus der Spalte `Gesamt Abweichung` der Zeitreihe — es gibt keine zweite
Kopie der Zahlen, die auseinanderlaufen könnte.

Ganz unten steht **„Befunde je Regel"** — eine Zeile je Prüfregel, auch für die, die nichts
gefunden haben. Eine `0` ist dort eine Aussage: Sie unterscheidet „keine Dubletten in den
Daten" von „gar nicht auf Dubletten geprüft".

Der Block löst ein Sichtbarkeitsproblem. Der Datenqualitäts-Tab ist nach Zeilennummer
sortiert, also liegen alle Regelarten durchmischt. Auf den Testdaten meldete V09
(Zukunftsmonat) 302 Warnungen und V08 (Ausreißer) neun — und die neun waren die
gefährlichen, weil sie die gesamte Abweichung trugen. Untereinander sieht man das Verhältnis
sofort.

Empfohlene Lesereihenfolge: **erst das Deckblatt.** Dort steht die Qualitätsquote — man
sollte wissen, auf wie vielen Zeilen die Zahlen beruhen, *bevor* man die Euro-Beträge liest.

Beim Lesen der DB-Rechnung immer die **Monatsspalte** mitnehmen: Ein Produkt mit neun von
zwölf Monaten ist nicht mit einem vollständigen vergleichbar. Der Bericht weist unter der
Tabelle eigens darauf hin.

### Der Block „Einfluss der beanstandeten Zeilen"

Steht auf dem Deckblatt direkt unter dem Betriebsergebnis und ist die wichtigste Zahl des
ganzen Berichts. Er beantwortet: *Wie viel von dieser Abweichung stammt aus Zeilen, die die
Prüfung beanstandet hat?*

Zeilen mit **WARNUNG** bleiben in der Rechnung — ein Ausreißer soll sichtbar bleiben, statt
stillschweigend geglättet zu werden. Das ist richtig, macht die Kopfzahl aber angreifbar.
Der reale Fall, der diesen Block ausgelöst hat:

```
Ist                              4.832.904,86 EUR     (+42 % über Plan)
Abweichung der Warnzeilen        1.446.228,88 EUR     101,2 % der Gesamtabweichung
Abweichung OHNE diese Zeilen        -17.704,02 EUR     das Geschäft lag UNTER Plan
```

Neun Zeilen mit einer Ist-Menge um das Zwanzig- bis Fünfundvierzigfache des Plans — eine
Ziffer zu viel — trugen die gesamte Abweichung. Jede Zahl war richtig gerechnet, und die
Kernaussage stand trotzdem falsch herum im Bericht.

Dreht sich ohne die beanstandeten Zeilen das Vorzeichen, schreibt der Bericht das
ausdrücklich dazu, auf dem Deckblatt und in der Konsole. Weggeworfen wird nichts: Beide
Zahlen stehen nebeneinander, die Entscheidung trifft der Leser.

## Datenformat

Komma-separiert, Punkt als Dezimaltrennzeichen, neun Spalten:

```csv
monat,produkt,kostenstelle,plan_menge,ist_menge,plan_preis,ist_preis,variable_stueckkosten,fixkosten_produkt
2026-02,Monitor 27 Zoll,Vertrieb Ost,300,312.0,280.0,274.90,160.0,4000
```

Kaputte Zeilen führen nicht zum Abbruch: Sie werden im Tab „Datenqualität" gemeldet,
aufsteigend nach Zeilennummer. Zeilen mit **FEHLER** fliegen aus der Rechnung, Zeilen mit
**WARNUNG** bleiben drin und werden markiert — siehe den Abschnitt oben.

Der Berichtsmonat ist der Monat der Berichtserstellung. Zeilen mit Ist-Werten für spätere
Monate bekommen eine Warnung (V09): Für einen Monat, der noch nicht vorbei ist, kann es
keine Ist-Zahlen geben.

## Entwickeln

```bash
gradlew build          # übersetzen + alle Tests
gradlew test           # nur Tests
gradlew run --args="daten.csv --kein-oeffnen"
```

`gradlew` statt `gradle`: Der Wrapper legt die Gradle-Version fest, sodass das Projekt ohne
installiertes Gradle baut und auf jedem Rechner dieselbe Version benutzt.

**Bei Widersprüchen zwischen IDE und `gradlew build` hat Gradle recht.** Zeigt der Editor
Fehler, die der Build nicht kennt, ist sein Klassenpfad veraltet — in VS Code hilft
`Java: Clean Java Language Server Workspace`.

### Testdaten sind getrennt von Arbeitsdaten

Die Tests lesen **ausschließlich** `src/test/resources/testdaten.csv`, nie eine
Arbeitsdatei. Diese Datei enthält bewusst alle vier Fehlerarten und **ändert sich nie**.

Der Grund ist teuer gelernt: Anfangs testeten sechs Klassen gegen die jeweils aktuelle
Rohdatendatei, mit Sätzen wie `assertEquals(4, produkte.size())`. Das ist eine Aussage über
die **Datei**, nicht über den **Code** — und wurde bei jedem neuen Datensatz falsch. Ergebnis
waren vierzehn rote Tests bei völlig intaktem Programm.

> **Regel:** Ein Test darf nichts voraussetzen, was sich ändern darf, ohne dass der Code
> falsch wird.

Aus demselben Grund stehen in den Javadocs keine konkreten Zeilennummern mehr.
Rechenbeispiele mit Zahlen sind in Ordnung — die stehen für sich.

Alles, was ein Datum braucht, bekommt es **übergeben**, nie aus der Uhr: `Validator` den
Berichtsmonat, `BerichtsmodellBauer` den Zeitstempel. Ein Test, der `now()` benutzt, wird
irgendwann von selbst rot, und dann weiß niemand mehr, ob der Code kaputt ist oder nur der
Kalender weitergelaufen.

## Aufbau

| Paket | Aufgabe |
|---|---|
| `config` | Kommandozeile einlesen, Vorgaben setzen |
| `io` | CSV lesen — verändert nichts, prüft nichts |
| `model` | `Rohzeile` (alles String) und `Datenzeile` (geprüft, `BigDecimal`) |
| `pruefung` | neun Regeln V01–V09, `Validator`, `Pruefprotokoll` |
| `rechnung` | Deckungsbeiträge, Aggregation je Produkt / Monat / Kostenstelle |
| `abweichung` | Preis-, Mengen-, Mischeffekt; Wesentlichkeit und Ampel |
| `report` | `Berichtsmodell` (was ist wahr) plus Konsolen- und Excel-Ausgabe |

Drei Regeln, die den Aufbau tragen:

- **Geld ist `BigDecimal`, nie `double`.** Gerechnet wird exakt; `double` entsteht erst beim
  Schreiben in Excel, das intern nichts anderes kennt.
- **Das `Berichtsmodell` weiß, *was* wahr ist; die Writer entscheiden, *wie* es aussieht.**
  Deshalb liefern Konsole und Excel dieselben Zahlen, ohne dass die Logik doppelt existiert.
- **Fehler haben verschiedene Besitzer.** Datenfehler werden berichtet (`Befund`),
  Programmierfehler knallen (`Datenzeile`), Bedienfehler bekommen einen Klartextsatz
  (`Berichtskonfiguration`). Dieselbe Situation, drei verschiedene richtige Antworten.

## Wie die Zahlen zustande kommen

Die Formeln und Regeln stehen im Code, jeweils im Javadoc der zuständigen Klasse:

| Frage | Klasse |
|---|---|
| Welche neun Prüfregeln gibt es? | `pruefung/regeln/` — je Regel eine Klasse mit ihrer ID |
| Warum meldet eine kaputte Zelle nur EINEN Befund? | Schweigegrundsatz, in den Regelklassen |
| Wie werden DB I und DB II gerechnet? | `DeckungsbeitragsRechner.rechne` |
| Wie wird die Abweichung zerlegt? | `AbweichungsRechner.jeZeile` |
| Was heißt ein positives Vorzeichen? | `Abweichung` — Plus ist ergebnisverbessernd |
| Wann wird gerundet? | `DeckungsbeitragsRechner.rechne` |
| Ab wann ist eine Abweichung wesentlich? | `Wesentlichkeit.bewerte` |
| Woher kommt die Euro-Schwelle ohne Angabe? | `Wesentlichkeit.fuer` |
| Wie belastbar ist die Kernaussage? | `Warnzeileneinfluss` |
| Warum zeigt das Diagramm eine Linie und nicht zwei? | `ExcelReportWriter.abweichungsdiagramm` |
| Warum stehen Regeln mit 0 Befunden in der Übersicht? | `Regelzaehlung` |

## Bewusst nicht gebaut

Der Code zeigt, was da ist — er kann nicht zeigen, was jemand absichtlich weggelassen hat.
Diese Liste beantwortet die Frage „vergessen oder Absicht?":

| | warum nicht |
|---|---|
| **Datenbank statt CSV** | Die Datenquelle liefert Dateien. Eine Datenbank würde nur die `io`-Schicht betreffen — der Rest bliebe unverändert. |
| **Mehrstufige Fixkostendeckung (DB III/IV)** | Die CSV kennt nur produktfixe Kosten. Bereichs- und Unternehmensfixkosten wären im `ProduktRechner` abzuziehen, sobald es sie gibt. |
| **Forecast / Hochrechnung** | Braucht eine Annahme über die Zukunft. Der Bericht stellt fest, was war — das ist eine andere Aufgabe. |
| **Ausreißer automatisch verwerfen** | Würde die Kernaussage stillschweigend reparieren. Der Bericht zeigt stattdessen beide Zahlen und lässt den Leser entscheiden — siehe „Einfluss der beanstandeten Zeilen". |
| **PDF-Ausgabe** | Excel ist das Format, in dem weitergearbeitet wird. Ein PDF erzeugt man daraus in zwei Klicks. |
| **Web-Frontend** | Ein Bericht je Monat rechtfertigt keinen Server. |
| **Mehrere Währungen** | Alle Beträge sind Euro. Währungen einzuführen hieße, jeden Betrag mit seiner Währung zu führen und Umrechnungskurse zu datieren — ein eigenes Thema. |
| **Weitere Diagramme** | Das Deckblatt trägt eines — die Abweichung je Monat. Für alles andere (einzelne Produkte, Kostenstellen) in der Zeitreihe die gewünschten Spalten markieren und `Alt+F1` drücken. Je Auswertung ein fest verdrahtetes Diagramm zu bauen kostet mehr, als es einbringt. |
| **Anführungszeichen im CSV-Parser** | Siehe Javadoc von `CsvEinleser`. Kommt eine Quelle mit Kommas in Textfeldern dazu, gehört dort eine CSV-Bibliothek her. |
