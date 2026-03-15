# User Stories – GUI-Redesign & One-Click-Analyse-Pipeline

**CodeSmellsDetector — Bachelorarbeit Kaufmann**
Stand: März 2026 | Sprint-Planung

---

## Epic-Übersicht

Das Ziel ist eine Swing-basierte Oberfläche, die den gesamten Analyse-Workflow streamlined: Projekt auswählen, alle drei Tools automatisch laufen lassen, Ergebnisse in einer Tabelle anzeigen — ohne CLI-Kenntnisse. Die bestehende `MainWindow`-Klasse wird grundlegend überarbeitet.

| Epic | Stories | Fokus |
|------|---------|-------|
| E1: Projekt-Setup | S-01 – S-03 | Projektauswahl, Konfiguration, Validierung |
| E2: Automatische Analyse | S-04 – S-07 | One-Click-Run, Fortschritt, Tool-Orchestrierung |
| E3: Ergebnis-Darstellung | S-08 – S-11 | Tabelle, Agreement, Export, Fehleranalyse |
| E4: Labeling-Unterstützung | S-12 – S-13 | Inline-Labeling, Evaluation |

---

## Epic 1: Projekt-Setup & Konfiguration

### S-01: Projektverzeichnis auswählen

| Priorität | Aufwand (SP) | Abhängigkeit |
|-----------|-------------|--------------|
| Must-Have | 3 SP | — |

**Als** Anwender **möchte ich** ein Java-Projektverzeichnis per Datei-Dialog oder Drag-and-Drop auswählen können, **damit** ich schnell zwischen verschiedenen Projekten wechseln kann, ohne Pfade manuell einzutippen.

**Beschreibung:** Der bestehende `projectPathField` wird um einen JFileChooser-Button und Drag-and-Drop-Support erweitert. Nach Auswahl wird automatisch geprüft, ob das Verzeichnis Java-Dateien enthält (via `SourceScanner`). Zusätzlich wird ein Dropdown mit den letzten 5 verwendeten Projekten angezeigt (gespeichert in einer lokalen Preferences-Datei).

**Akzeptanzkriterien:**
- [ ] JFileChooser öffnet sich bei Klick auf „Durchsuchen" und filtert auf Verzeichnisse
- [ ] Drag-and-Drop eines Verzeichnisses auf das Feld setzt den Pfad
- [ ] Nach Auswahl: automatische Validierung — grünes Häkchen bei gültigem Java-Projekt, rotes X bei Fehler
- [ ] Validierung prüft: (a) Verzeichnis existiert, (b) enthält .java-Dateien, (c) pom.xml oder build.gradle vorhanden
- [ ] Dropdown zeigt die letzten 5 Projekte (persistent via `java.util.prefs.Preferences`)
- [ ] Fehlermeldung als Tooltip bei ungültigem Pfad

**Technische Tasks:**
1. JFileChooser mit `DIRECTORIES_ONLY` in MainWindow einbauen
2. DropTarget für Drag-and-Drop auf projectPathField implementieren
3. `ProjectValidator`-Klasse erstellen (prüft .java-Files, Build-Datei)
4. `RecentProjectsManager` (Preferences-basiert, max 5 Einträge)
5. Visuelles Feedback: Icon neben Pfadfeld (valid/invalid)

---

### S-02: Tool-Konfiguration übersichtlich darstellen

| Priorität | Aufwand (SP) | Abhängigkeit |
|-----------|-------------|--------------|
| Must-Have | 5 SP | S-01 |

**Als** Anwender **möchte ich** die Konfiguration aller drei Tools (Baseline, SonarQube, JDeodorant) in einem übersichtlichen Panel sehen und anpassen können, **damit** ich verstehe, welche Tools mit welchen Einstellungen laufen werden.

**Beschreibung:** Ein konfiguriertes TabbedPane oder Accordion-Panel zeigt pro Tool eine Sektion: Baseline (Schwellenwerte), SonarQube (Host, Token, Docker-Toggle), JDeodorant (CSV-Pfad). Jedes Tool hat eine Checkbox zum Aktivieren/Deaktivieren. Die bestehenden Felder (`sonarHostField`, `sonarTokenField`, `methodFieldSpinner`, `depTypeSpinner`, `jdeodorantCsvField`) werden in diese Struktur migriert.

**Akzeptanzkriterien:**
- [ ] Drei Tool-Sektionen sichtbar: Baseline, SonarQube, JDeodorant
- [ ] Jede Sektion hat eine Aktivierungs-Checkbox
- [ ] Baseline-Sektion zeigt: WMC-Schwelle, TCC-Schwelle, ATFD/CBO-Schwellen + methodPlusField und dependencyType Spinner
- [ ] SonarQube-Sektion zeigt: Host-URL, Token (maskiert), Docker-Checkbox, Regel (S6539, read-only)
- [ ] JDeodorant-Sektion zeigt: CSV-Pfad mit Datei-Dialog
- [ ] Mindestens ein Tool muss aktiviert sein (sonst Run-Button deaktiviert)
- [ ] Default-Werte aus Exposé vorausgefüllt (WMC≥47, TCC<0.33, ATFD>5/CBO≥20)

**Technische Tasks:**
1. `ToolConfigPanel` erstellen (JTabbedPane mit 3 Tabs)
2. `BaselineConfigTab`: Spinner für alle Schwellenwerte, Labels mit Erklärung
3. `SonarConfigTab`: Host/Token-Felder migrieren, Docker-Toggle, Health-Indicator
4. `JDeodorantConfigTab`: CSV-JFileChooser, Validierung (.csv-Datei existiert)
5. Aktivierungs-Checkboxen mit Listener → Run-Button-State aktualisieren
6. Bestehende MainWindow-Felder in neues Layout migrieren

---

### S-03: Output-Verzeichnis und Projektname konfigurieren

| Priorität | Aufwand (SP) | Abhängigkeit |
|-----------|-------------|--------------|
| Should-Have | 2 SP | S-01 |

**Als** Anwender **möchte ich** ein Output-Verzeichnis wählen und einen Projektnamen vergeben können, **damit** die Ergebnisse sauber pro Projekt getrennt gespeichert werden.

**Beschreibung:** Der Projektname wird automatisch aus dem Verzeichnisnamen abgeleitet (editierbar). Das Output-Verzeichnis erhält einen Unterordner pro Projekt (z.B. `output/commons-collections/`). Ein Info-Label zeigt den resultierenden Pfad an.

**Akzeptanzkriterien:**
- [ ] Projektname wird automatisch aus Verzeichnisname vorgeschlagen
- [ ] Projektname ist manuell editierbar
- [ ] Output-Verzeichnis per JFileChooser wählbar
- [ ] Angezeigter Ergebnis-Pfad: `{outputDir}/{projectName}/`
- [ ] Verzeichnis wird automatisch erstellt wenn nicht vorhanden

**Technische Tasks:**
1. ProjectNameField mit Auto-Suggest aus `Path.getFileName()`
2. `OutputPathResolver`: kombiniert outputDir + projectName
3. `ResultExporter.createOutputDir()` aufrufen bei Run-Start
4. Info-Label mit dynamischer Pfad-Anzeige

---

## Epic 2: Automatische Analyse (One-Click-Run)

### S-04: One-Click-Analyse starten

| Priorität | Aufwand (SP) | Abhängigkeit |
|-----------|-------------|--------------|
| Must-Have | 5 SP | S-02 |

**Als** Anwender **möchte ich** mit einem einzigen Klick auf „Analyse starten" alle aktivierten Tools nacheinander auf dem ausgewählten Projekt laufen lassen können, **damit** ich nicht manuell CLI-Befehle eingeben oder Tools einzeln starten muss.

**Beschreibung:** Der zentrale „Analyse starten"-Button ruft `AnalysisOrchestrator.run()` in einem SwingWorker-Thread auf. Die GUI bleibt responsiv. Konfiguration wird aus den UI-Feldern gelesen und in `BaselineThresholds`, `SonarConfig` und `ProjectConfig` gemappt. Nach Abschluss werden die Ergebnisse automatisch in der Tabelle angezeigt (siehe S-08).

**Akzeptanzkriterien:**
- [ ] Button „Analyse starten" ist sichtbar und prominent platziert
- [ ] Button ist deaktiviert solange kein gültiges Projekt ausgewählt oder kein Tool aktiviert ist
- [ ] Klick startet die Analyse in einem Hintergrund-Thread (kein UI-Freeze)
- [ ] Während der Analyse: Button zeigt „Läuft..." und ist deaktiviert
- [ ] Bei Fehler: Dialog mit Fehlermeldung, Log-Verweis
- [ ] Bei Erfolg: Automatischer Wechsel zur Ergebnis-Ansicht
- [ ] `AnalysisOrchestrator.run()` wird mit korrekten Parametern aufgerufen

**Technische Tasks:**
1. RunButton mit ActionListener erstellen
2. `ConfigMapper`: UI-Felder → `BaselineThresholds` + `SonarConfig` + `ProjectConfig`
3. `SwingWorker<List<CandidateDTO>, String>` implementieren
4. `done()`-Callback: Ergebnis an Tabelle übergeben (S-08)
5. Fehlerbehandlung: Exception → `JOptionPane.showErrorDialog`
6. Button-State-Management (enabled/disabled/running)

---

### S-05: Fortschrittsanzeige während Analyse

| Priorität | Aufwand (SP) | Abhängigkeit |
|-----------|-------------|--------------|
| Should-Have | 3 SP | S-04 |

**Als** Anwender **möchte ich** während der Analyse sehen können, welcher Schritt gerade läuft und wie weit der Prozess ist, **damit** ich weiß, dass das Tool arbeitet und abschätzen kann, wie lange es noch dauert.

**Beschreibung:** Eine JProgressBar und ein Status-Label zeigen den aktuellen Schritt an. Der SwingWorker publiziert Zwischenstatus über `publish()`/`process()`. Die Schritte sind: (1) Java-Dateien scannen, (2) Baseline-Analyse, (3) SonarQube-Scan (falls aktiviert), (4) JDeodorant-Import (falls aktiviert), (5) Merge & Export.

**Akzeptanzkriterien:**
- [ ] Fortschrittsbalken zeigt prozentualen Fortschritt (0–100%)
- [ ] Status-Label zeigt aktuellen Schritt (z.B. „SonarQube-Scan läuft...")
- [ ] Mindestens 5 Zwischenstatus werden angezeigt
- [ ] Bei deaktivierten Tools werden deren Schritte übersprungen (Balken springt)
- [ ] Log-Bereich (JTextArea, scrollbar) zeigt detaillierte Ausgaben

**Technische Tasks:**
1. `ProgressPanel`: JProgressBar + JLabel + JTextArea (scrollable)
2. `SwingWorker.publish()` für Zwischenstatus in AnalysisOrchestrator einbauen
3. `ProgressCallback`-Interface: `onStepStarted(String step, int percent)`
4. `AnalysisOrchestrator` um Callback-Parameter erweitern
5. Log-Output umleiten: LOGGER-Nachrichten in JTextArea schreiben (via Handler)

---

### S-06: SonarQube-Docker automatisch starten/stoppen

| Priorität | Aufwand (SP) | Abhängigkeit |
|-----------|-------------|--------------|
| Could-Have | 3 SP | S-04 |

**Als** Anwender **möchte ich** SonarQube per Docker automatisch starten und nach der Analyse stoppen lassen können, ohne Docker-Befehle kennen zu müssen, **damit** ich SonarQube nutzen kann, ohne Docker-Expertise zu benötigen.

**Beschreibung:** Wenn SonarQube aktiviert und Docker-Modus an ist, startet die Pipeline automatisch `SonarDockerManager.startContainer()`, wartet auf Health-Check (`SonarHealthClient.waitForReady()`), führt den Scan durch und stoppt den Container danach. Ein Indikator in der GUI zeigt den Docker-Status an.

**Akzeptanzkriterien:**
- [ ] Bei aktiviertem Docker-Toggle: Container wird automatisch gestartet vor dem Scan
- [ ] Health-Check wartet bis SonarQube ready ist (mit Timeout)
- [ ] Nach Scan: Container wird gestoppt (konfigurierbar: Auto-Stop an/aus)
- [ ] Status-Indikator: „Docker: gestartet / bereit / gestoppt"
- [ ] Bei Docker-Fehler: klare Meldung („Docker nicht installiert" o.ä.)
- [ ] Timeout nach 120s mit Abbruch-Option

**Technische Tasks:**
1. `DockerStatusIndicator`-Widget (grün/gelb/rot mit Label)
2. `SonarDockerManager.startContainer()` + `SonarHealthClient.waitForReady()` in Pipeline integrieren
3. Auto-Stop-Checkbox in SonarConfigTab
4. Docker-Präsenz-Check: `docker info` ausführen, Fehler abfangen
5. Timeout-Handling im SwingWorker mit `cancel()`-Support

---

### S-07: Analyse abbrechen

| Priorität | Aufwand (SP) | Abhängigkeit |
|-----------|-------------|--------------|
| Should-Have | 2 SP | S-04 |

**Als** Anwender **möchte ich** eine laufende Analyse jederzeit abbrechen können, **damit** ich nicht warten muss, wenn ich merke, dass die Konfiguration falsch war.

**Beschreibung:** Ein „Abbrechen"-Button erscheint während der Analyse. Er ruft `SwingWorker.cancel(true)` auf und beendet laufende Prozesse sauber. Teilresultate werden verworfen.

**Akzeptanzkriterien:**
- [ ] Abbrechen-Button ist nur während laufender Analyse sichtbar/aktiv
- [ ] Klick stoppt den SwingWorker
- [ ] Laufende SonarQube-Scans werden per `Process.destroy()` beendet
- [ ] Status-Anzeige wechselt auf „Abgebrochen"
- [ ] Teilweise erzeugte Output-Dateien werden aufgeräumt

**Technische Tasks:**
1. Cancel-Button neben Run-Button platzieren (nur sichtbar während Run)
2. `SwingWorker.cancel(true)` + `isCancelled()`-Checks in Pipeline-Schritten
3. `InterruptedException` handling in AnalysisOrchestrator
4. Cleanup: temporäre Dateien löschen bei Abbruch

---

## Epic 3: Ergebnis-Darstellung

### S-08: Kandidaten-Tabelle anzeigen

| Priorität | Aufwand (SP) | Abhängigkeit |
|-----------|-------------|--------------|
| Must-Have | 5 SP | S-04 |

**Als** Anwender **möchte ich** nach der Analyse eine übersichtliche Tabelle aller Kandidatenklassen sehen, mit Spalten für Klassenname, Tool-Flags und Metriken, **damit** ich sofort sehen kann, welche Klassen von welchen Tools als God Class erkannt wurden.

**Beschreibung:** Eine JTable zeigt die Ergebnisse aus `List<CandidateDTO>` an. Spalten: FQCN, Baseline (✓/✗), SonarQube (✓/✗), JDeodorant (✓/✗), WMC, TCC, ATFD/CBO, LOC, Methods, Fields, DependencyTypes. Die Tabelle ist sortierbar (Klick auf Spaltenheader), filterbar und farblich kodiert (God Class = rot hinterlegt, Kandidat bei ≥2 Tools = orange).

**Akzeptanzkriterien:**
- [ ] JTable mit allen CandidateDTO-Feldern als Spalten
- [ ] Tool-Flag-Spalten zeigen ✓ (grün) / ✗ (grau) als Zell-Renderer
- [ ] Tabelle ist nach jeder Spalte sortierbar (TableRowSorter)
- [ ] Farbkodierung: Zeilen mit baselineFlag=true rot/orange hinterlegt
- [ ] Zeilen mit ≥2 Tool-Flags sind hervorgehoben (orange)
- [ ] Statusleiste zeigt: „42 Kandidaten | 12 Baseline | 8 SonarQube | 15 JDeodorant"
- [ ] Tabelle wird automatisch nach Analyse befüllt

**Technische Tasks:**
1. `CandidateTableModel extends AbstractTableModel` erstellen
2. Spalten-Definition: Name, 3× Tool-Flags, Metriken-Spalten
3. `BooleanCellRenderer`: ✓/✗ mit Farbe
4. `RowColorRenderer`: Zeilen-Hintergrund basierend auf Flag-Kombination
5. `TableRowSorter` mit Comparatoren pro Spaltentyp
6. StatusBar-Panel mit Zählern (berechnet aus CandidateDTO-Liste)
7. Integration: `SwingWorker.done()` → `tableModel.setData(candidates)`

---

### S-09: Tabelle filtern und durchsuchen

| Priorität | Aufwand (SP) | Abhängigkeit |
|-----------|-------------|--------------|
| Should-Have | 3 SP | S-08 |

**Als** Anwender **möchte ich** die Kandidaten-Tabelle nach Tool-Kombination, Metrik-Bereichen oder Klassennamen filtern können, **damit** ich schnell die relevanten Klassen finde (z.B. alle Klassen, die nur von einem Tool erkannt wurden).

**Beschreibung:** Ein Filter-Panel oberhalb der Tabelle bietet: (a) Checkboxen für Tool-Flags (zeige nur Baseline-Kandidaten, nur SonarQube etc.), (b) ein Textfeld für Klassenname-Suche, (c) Vordefinierte Quick-Filter („Alle Tools einig", „Nur 1 Tool", „Disagreement").

**Akzeptanzkriterien:**
- [ ] Textfeld für Freitextsuche im FQCN (Echtzeit-Filter)
- [ ] Checkboxen: „Baseline", „SonarQube", „JDeodorant" (kombinierbar)
- [ ] Quick-Filter-Buttons: „Alle einig" / „Nur 1 Tool" / „Disagreement (2 von 3)"
- [ ] Filter werden per RowFilter auf TableRowSorter angewandt
- [ ] Zähler in Statusleiste aktualisiert sich mit Filter
- [ ] Filter-Reset-Button („Alle anzeigen")

**Technische Tasks:**
1. `FilterPanel`: JTextField + 3 JCheckBox + 3 Quick-Filter-Buttons
2. `CompositeRowFilter`: kombiniert Text- und Flag-Filter per AND/OR
3. Quick-Filter-Logik: allAgree = alle 3 Flags gleich, etc.
4. Listener auf alle Filter-Elemente → `tableRowSorter.setRowFilter()`
5. StatusBar-Update bei Filteränderung

---

### S-10: Tool-Agreement-Übersicht anzeigen

| Priorität | Aufwand (SP) | Abhängigkeit |
|-----------|-------------|--------------|
| Should-Have | 5 SP | S-08 |

**Als** Anwender **möchte ich** eine visuelle Übersicht der Tool-Übereinstimmung sehen (Jaccard-Index, Venn-Diagramm), **damit** ich F2 (Agreement) direkt in der GUI beantworten kann.

**Beschreibung:** Ein separater Tab „Agreement" zeigt: (a) eine Jaccard-Matrix als Tabelle (3×3), (b) Zähler: both/onlyA/onlyB pro Paar, (c) ein einfaches Venn-Diagramm (als gerenderte Grafik oder als Zahlenaufstellung). Daten kommen aus `tool_agreement.csv` bzw. werden live aus den CandidateDTO-Flags berechnet.

**Akzeptanzkriterien:**
- [ ] Agreement-Tab ist nach Analyse sichtbar
- [ ] Jaccard-Matrix als formatierte Tabelle (3×3) mit farbkodiertem Wert
- [ ] Pro Tool-Paar: both, onlyA, onlyB als Detailzeile
- [ ] Venn-Diagramm-Darstellung (mindestens als Zahlenaufstellung, idealerweise grafisch)
- [ ] Daten werden live aus den Kandidaten berechnet (kein CSV-Import nötig)

**Technische Tasks:**
1. `AgreementPanel` erstellen
2. `AgreementCalculator`: berechnet Jaccard + Overlap aus `List<CandidateDTO>`
3. `JaccardMatrixTable`: 3×3 Tabelle mit farbkodiertem Hintergrund
4. `VennDiagramPanel`: Custom JPanel mit `paintComponent()` für 3-Kreis-Venn
5. Tab in Ergebnis-TabbedPane einhängen

---

### S-11: Ergebnisse exportieren

| Priorität | Aufwand (SP) | Abhängigkeit |
|-----------|-------------|--------------|
| Must-Have | 3 SP | S-08 |

**Als** Anwender **möchte ich** die Ergebnisse (Tabelle, Agreement, Metriken) als CSV, JSON und Markdown-Report per Knopfdruck exportieren können, **damit** ich die Daten für die Bachelorarbeit weiterverwenden kann.

**Beschreibung:** Ein „Exportieren"-Button öffnet einen Dialog mit Optionen: CSV (results.csv), JSON (results.json), Labeling-CSV (labeling_input.csv), Markdown-Report. Nutzt die bestehenden `ResultExporter` und `GroundTruthExporter` Klassen.

**Akzeptanzkriterien:**
- [ ] Export-Button in der Toolbar sichtbar nach Analyse
- [ ] Dialog mit Checkboxen: CSV, JSON, Labeling-CSV, Report
- [ ] Dateien werden in das konfigurierte Output-Verzeichnis geschrieben
- [ ] Erfolgsmeldung mit Pfadangabe nach Export
- [ ] Bestehende Dateien: Rückfrage vor Überschreiben

**Technische Tasks:**
1. `ExportDialog`: JDialog mit Checkbox-Optionen
2. `ResultExporter.writeCsv()` / `writeJson()` aufrufen
3. `GroundTruthExporter.exportLabelingCsv()` aufrufen
4. `ReportGenerator.generateReport()` aufrufen (nur nach Evaluation)
5. Überschreibschutz: `Files.exists()` → Confirm-Dialog

---

## Epic 4: Labeling-Unterstützung

### S-12: Inline-Labeling in der Tabelle

| Priorität | Aufwand (SP) | Abhängigkeit |
|-----------|-------------|--------------|
| Must-Have | 8 SP | S-08 |

**Als** Anwender **möchte ich** die Labeling-Kriterien (K1–K4, Kommentar, finalLabel) direkt in der Kandidaten-Tabelle ausfüllen können, **damit** ich nicht zwischen GUI und Excel wechseln muss, um die Ground Truth zu erstellen.

**Beschreibung:** Zusätzliche Spalten in der JTable: K1, K2, K3, K4 (Checkbox), Kommentar (Textfeld), finalLabel (Dropdown: true/false/uncertain). Die Kriterien-Spalten sind editierbar. Ein „Speichern"-Button exportiert die gelabelten Daten als `labeling_input.csv`. Ein Auto-Label-Feature berechnet `finalLabel` aus ≥3/4 Kriterien.

**Akzeptanzkriterien:**
- [ ] Spalten K1–K4 sind als Checkboxen editierbar
- [ ] Spalte „Kommentar" ist als Textfeld editierbar
- [ ] Spalte „finalLabel" ist als Dropdown editierbar (true/false/uncertain)
- [ ] Auto-Label-Button: setzt finalLabel = true wenn ≥3/4 Kriterien erfüllt
- [ ] „Speichern" exportiert als labeling_input.csv im erwarteten Format
- [ ] „Laden" importiert bereits gelabelte CSV zurück in die Tabelle
- [ ] Unsaved-Changes-Warnung beim Schließen

**Technische Tasks:**
1. `CandidateTableModel` um K1–K4, comment, finalLabel Spalten erweitern
2. Custom CellEditors: CheckboxEditor (K1–K4), TextEditor (comment), ComboBoxEditor (finalLabel)
3. `AutoLabelAction`: iteriert Zeilen, zählt true-Kriterien, setzt finalLabel
4. `LabelPersistence`: Speichern → CSV (kompatibel mit `LabelCsvImporter`), Laden → Merge mit Kandidaten
5. DirtyFlag für Unsaved-Changes-Warnung (WindowListener)
6. Integration mit `GroundTruthExporter.exportLabelingCsv()`

---

### S-13: Evaluation aus GUI starten

| Priorität | Aufwand (SP) | Abhängigkeit |
|-----------|-------------|--------------|
| Must-Have | 5 SP | S-12 |

**Als** Anwender **möchte ich** die Evaluation (Precision/Recall/F1, Agreement, FP/FN-Analyse) direkt aus der GUI starten können, nachdem das Labeling abgeschlossen ist, **damit** ich die Ergebnisse sofort sehen kann, ohne den CLI-Evaluate-Modus nutzen zu müssen.

**Beschreibung:** Ein „Evaluieren"-Button (aktiv nur wenn Labels vorhanden) ruft `AnalysisOrchestrator.runEvaluation()` auf. Die Ergebnisse werden in neuen Tabs angezeigt: (a) Accuracy-Tabelle (P/R/F1/MCC pro Tool), (b) Agreement-Matrix, (c) FP/FN-Listen. Optional: Zweiter-Reviewer-CSV laden für Reliabilitätsberechnung.

**Akzeptanzkriterien:**
- [ ] „Evaluieren"-Button ist aktiv wenn mindestens ein finalLabel gesetzt ist
- [ ] Accuracy-Tab zeigt Precision, Recall, F1, Specificity, MCC pro Tool
- [ ] FP/FN-Tab zeigt False-Positive- und False-Negative-Listen pro Tool
- [ ] Zweiter-Reviewer-CSV kann geladen werden für Reliabilitätsberechnung
- [ ] Bei geladenem Second Review: Reliability-Tab mit κ, AC1, Raw Agreement
- [ ] Alle Evaluation-Dateien werden ins Output-Verzeichnis geschrieben

**Technische Tasks:**
1. EvaluateButton mit Aktiv-Check (`hasLabels()`)
2. SwingWorker für `runEvaluation()` (analog zu S-04)
3. `AccuracyPanel`: Tabelle mit `EvaluationMetrics` pro Tool
4. `FpFnPanel`: zwei JTables (FP-Liste, FN-Liste) pro Tool-Tab
5. `SecondReviewLoader`: JFileChooser → `SecondReviewEvaluator.evaluate()`
6. `ReliabilityPanel`: ReliabilityMetrics anzeigen (κ, AC1, Agreement)
7. Alle Panels in Ergebnis-TabbedPane als neue Tabs

---

## Priorisierung & Story Map

**Must-Have (MVP, Sprint 1–2):** S-01, S-02, S-04, S-08, S-11, S-12, S-13 = 34 SP

**Should-Have (Sprint 3):** S-03, S-05, S-07, S-09, S-10 = 13 SP

**Could-Have (Backlog):** S-06 = 3 SP

**Gesamt:** 50 Story Points über 13 Stories

### Abhängigkeitsgraph

```
S-01 → S-02 → S-04 → S-05, S-06, S-07
S-04 → S-08 → S-09, S-10, S-11
S-08 → S-12 → S-13
```

*Der kritische Pfad ist: S-01 → S-02 → S-04 → S-08 → S-12 → S-13 (26 SP). Diese Stories sollten zuerst umgesetzt werden.*

---

## Technische Randbedingungen

- **Framework:** Java Swing (konsistent mit bestehendem MainWindow)
- **Threading:** Alle Tool-Runs in SwingWorker, niemals im EDT
- **Bestehender Code:** AnalysisOrchestrator, ResultExporter, alle Analyzer-Klassen bleiben unverändert — GUI ist eine neue Schicht darüber
- **Package:** Neue Klassen in `org.example.gui` (bestehend) und `org.example.gui.panels` (neu)
- **CandidateDTO:** Zentrales DTO, wird NICHT geändert (siehe Context_Code.md)
- **Testing:** Unit-Tests für ConfigMapper, AgreementCalculator, CompositeRowFilter; GUI-Tests optional

# Feature-Backlog: Offene Stories

**Basiert auf:** Code-Review März 2026 vs. Exposé-Anforderungen  
**Konvention:** Gleiche Struktur wie PROMPTS.md / AGENTS.md  
**Regel:** baseline/, sonar/, jdeodorant/ Packages werden NICHT verändert – nur neue Klassen oder Erweiterungen in metrics/, orchestrator/, cli/, reporting/

---

## Übersicht & Abhängigkeiten

```
US-15 ─────────────────────────────────────── (unabhängig, P1)
US-16 ──→ US-17 ──→ US-18 ──→ US-19         (Kern-Kette, P0)
US-20 ─────────────────────────────────────── (unabhängig, P2)
US-21 ─────────────────────────────────────── (unabhängig, P2)
```

| Story | Prio | SP | Abhängigkeit | Beschreibung |
|-------|------|----|-------------|--------------|
| US-15 | P1   | 3  | keine       | Bootstrap-Konfidenzintervalle für MetricsEngine |
| US-16 | P0   | 2  | keine       | CLI: Multi-Projekt-Batch-Modus |
| US-17 | P0   | 3  | US-16       | Multi-Projekt-Aggregation (Micro/Macro-Average) |
| US-18 | P0   | 2  | US-17       | ReportGenerator: Aggregierten Report erzeugen |
| US-19 | P0   | 1  | US-17       | CLI: --aggregate Flag für Gesamtauswertung |
| US-20 | P2   | 2  | keine       | Sensitivitätsanalyse (Label-Threshold parametrierbar) |
| US-21 | P2   | 1  | keine       | Repo-Hygiene: Aufräumen für Abgabe |

**Geschätzter Gesamtaufwand:** ~14 Story Points (~3–4 Arbeitstage)

---

## US-15: Bootstrap-Konfidenzintervalle

**Priorität:** P1 – Im Exposé versprochen, stärkt statistische Aussagekraft  
**Story Points:** 3  
**Abhängigkeit:** Keine  
**Package:** `metrics/` (neue Klasse)

### Ziel

```
NEU: src/main/java/org/example/metrics/BootstrapConfidenceInterval.java
NEU: src/test/java/org/example/metrics/BootstrapConfidenceIntervalTest.java
ÄNDERN: src/main/java/org/example/metrics/EvaluationMetrics.java (optionale CI-Felder)
```

### Kontext

Das Exposé verspricht: „95%-Konfidenzintervalle via Bootstrap." `MetricsEngine.computeMetrics()` liefert bisher nur Punktschätzer. Bei kleinen Fallzahlen (79 Kandidaten, davon ~5–15 God Classes) sind CIs wissenschaftlich zwingend nötig.

### Spezifikation

```
Klasse: BootstrapConfidenceInterval
Package: org.example.metrics

public record ConfidenceInterval(double lower, double upper, double pointEstimate) {}

public record BootstrapResult(
    EvaluationMetrics pointEstimates,
    ConfidenceInterval precisionCI,
    ConfidenceInterval recallCI,
    ConfidenceInterval f1CI,
    ConfidenceInterval mccCI
) {}

public static BootstrapResult computeWithCI(
    Set<String> predicted,
    Set<String> actual,
    int totalItems,
    int nBootstrap,    // Default: 10000
    long seed          // Default: 42
)
```

**Algorithmus:**
1. Erstelle Liste aller Items (TP/FP/FN/TN-Zuordnung)
2. Für i = 1..nBootstrap:
    - Ziehe mit Zurücklegen (Bootstrap-Sample, Größe = totalItems)
    - Berechne P/R/F1/MCC auf dem Sample
3. Sortiere die nBootstrap Werte je Metrik
4. CI = [Perzentil 2.5%, Perzentil 97.5%]

**Seed:** `new Random(seed)` für Determinismus.

### Constraints

- `MetricsEngine` NICHT ändern – neue Klasse daneben
- `EvaluationMetrics` erweitern ist erlaubt (optionale CI-Felder, Rückwärtskompatibel)
- Keine neuen Maven-Dependencies

### Akzeptanzkriterien

```
□ BootstrapConfidenceInterval.computeWithCI() liefert BootstrapResult
□ Bei perfekter Übereinstimmung: CI = [1.0, 1.0] für P/R/F1
□ Bei totalItems=0: leere/sinnvolle Defaults, kein Crash
□ Seed 42 liefert bei gleichen Inputs identische CIs
□ Test mit bekanntem Setup: tp=5, fp=2, fn=3, tn=40 → CI-Breiten plausibel
□ mvn test grün
```

---

## US-16: CLI Multi-Projekt-Batch-Modus

**Priorität:** P0 – Exposé verspricht 3 Projekte  
**Story Points:** 2  
**Abhängigkeit:** Keine  
**Package:** `cli/` (Main erweitern)

### Ziel

```
ÄNDERN: src/main/java/org/example/cli/Main.java (neues --batch Flag)
NEU: src/test/java/org/example/cli/MainBatchTest.java (optional, CLI-Tests sind schwer)
```

### Kontext

Aktuell analysiert die CLI ein Projekt pro Aufruf. Für die Bachelorarbeit müssen 3 Projekte sequentiell durch die Pipeline. Ein Batch-Modus erspart manuelles Dreifach-Ausführen und dokumentiert den Gesamtlauf.

### Spezifikation

Neues CLI-Pattern:

```bash
# Batch-Analyse: 3 Projekte nacheinander
java -jar target/CodeSmellsDetector.jar --batch \
  --project /path/to/projectA --jdeodorant-csv /data/jd-A.csv \
  --project /path/to/projectB --jdeodorant-csv /data/jd-B.csv \
  --project /path/to/projectC --jdeodorant-csv /data/jd-C.csv \
  --run-sonar --output output

# Ergebnis-Struktur:
# output/projectA/results.csv, labeling_input.csv, ...
# output/projectB/results.csv, labeling_input.csv, ...
# output/projectC/results.csv, labeling_input.csv, ...
```

**Alternative (einfacher):** Kein neues Flag, sondern ein Shell-Skript `scripts/run_batch.sh` das 3× die CLI aufruft. Das ist pragmatischer und reicht für die Arbeit.

```bash
#!/bin/bash
# scripts/run_batch.sh
set -e
PROJECTS=("projectA" "projectB" "projectC")
PATHS=("/path/to/A" "/path/to/B" "/path/to/C")
JD_CSVS=("jd-A.csv" "jd-B.csv" "jd-C.csv")

mvn -q compile
for i in "${!PROJECTS[@]}"; do
  echo "=== ${PROJECTS[$i]} ==="
  java -cp target/classes org.example.cli.Main \
    --project "${PATHS[$i]}" \
    --run-sonar \
    --jdeodorant-csv "${JD_CSVS[$i]}" \
    --output "output/${PROJECTS[$i]}"
done
```

### Akzeptanzkriterien

```
□ Skript/Batch erzeugt pro Projekt ein Unterverzeichnis in output/
□ Jedes Unterverzeichnis enthält: results.csv, results.json, labeling_input.csv
□ Projekte werden sequentiell analysiert (kein Parallel-Problem mit SonarQube)
□ Skript bricht bei Fehler ab (set -e)
```

---

## US-17: Multi-Projekt-Aggregation

**Priorität:** P0 – Ohne Aggregation keine Gesamtaussage über 3 Projekte  
**Story Points:** 3  
**Abhängigkeit:** US-16 (3 Projekte müssen analysiert + evaluiert sein)  
**Package:** `reporting/` (neue Klasse)

### Ziel

```
NEU: src/main/java/org/example/reporting/MultiProjectAggregator.java
NEU: src/test/java/org/example/reporting/MultiProjectAggregatorTest.java
```

### Kontext

Die Evaluation läuft derzeit pro Projekt (eine `metrics_summary.csv` pro Projekt). Für die Bachelorarbeit braucht es:
- **Micro-Average:** TP/FP/FN/TN über alle Projekte summieren, dann P/R/F1/MCC berechnen
- **Macro-Average:** P/R/F1/MCC pro Projekt, dann Mittelwert + Standardabweichung

### Spezifikation

```java
package org.example.reporting;

public class MultiProjectAggregator {

    public record AggregatedMetrics(
        EvaluationMetrics microAverage,
        double macroPrecision, double macroRecall, double macroF1, double macroMcc,
        double stdPrecision, double stdRecall, double stdF1, double stdMcc,
        int projectCount
    ) {}

    /**
     * Aggregiert Metriken über mehrere Projekte.
     * @param perProject Map von Projektname → (Map von Toolname → EvaluationMetrics)
     * @return Map von Toolname → AggregatedMetrics
     */
    public static Map<String, AggregatedMetrics> aggregate(
            Map<String, Map<String, EvaluationMetrics>> perProject) { ... }

    /**
     * Exportiert aggregierte Metriken als CSV.
     * Header: tool,microP,microR,microF1,microMCC,macroP,macroR,macroF1,macroMCC,
     *         stdP,stdR,stdF1,stdMCC,projects
     */
    public static Path exportCsv(
            Map<String, AggregatedMetrics> aggregated, Path outputDir) throws IOException { ... }
}
```

### Akzeptanzkriterien

```
□ Micro-Average: korrekte Summation (tp_total = Σ tp_i)
□ Macro-Average: korrekte Mittelwerte (macroP = mean(P_i))
□ Standardabweichung: Populations-StdDev (nicht Stichproben-StdDev, da N=3 klein)
□ Bei 1 Projekt: Micro = Macro = Einzelwert, StdDev = 0
□ Bei 0 Projekten: leere Map, kein Crash
□ CSV-Export: deterministische Reihenfolge (baseline, jdeodorant, sonar)
□ mvn test grün
```

---

## US-18: Aggregierten Report erzeugen

**Priorität:** P0 – Gesamtübersicht für die Arbeit  
**Story Points:** 2  
**Abhängigkeit:** US-17  
**Package:** `reporting/` (ReportGenerator erweitern)

### Ziel

```
ÄNDERN: src/main/java/org/example/reporting/ReportGenerator.java (neue Methode)
ÄNDERN: src/test/java/org/example/reporting/ReportGeneratorTest.java (neuer Test)
```

### Spezifikation

Neue Methode in `ReportGenerator`:

```java
/**
 * Erzeugt aggregated_report.md mit Gesamtübersicht über alle Projekte.
 */
public Path generateAggregated(
    Map<String, AggregatedMetrics> aggregated,
    Map<String, Map<String, EvaluationMetrics>> perProject,
    Map<String, List<OverlapResult>> agreementPerProject,
    Path outputDir
) throws IOException { ... }
```

**Report-Struktur:**

```markdown
# God-Class Detection – Aggregated Report

## Per-Project Summary
| Project | Tool | P | R | F1 | MCC | TP | FP | FN | TN |
|---------|------|...|...|....|-----|----|----|----|----|

## Aggregated Metrics (Micro-Average)
| Tool | P | R | F1 | MCC |
|------|...|...|....|-----|

## Aggregated Metrics (Macro-Average ± StdDev)
| Tool | P | R | F1 | MCC |
|------|...|...|....|-----|

## Tool Agreement (per project)
...
```

### Akzeptanzkriterien

```
□ Report enthält alle Sektionen
□ Zahlen auf 4 Dezimalstellen formatiert (wie bestehender Report)
□ Macro-Zeile zeigt mean ± stddev (z.B. "0.7500 ± 0.1200")
□ mvn test grün
```

---

## US-19: CLI --aggregate Flag

**Priorität:** P0 – Verbindet US-17/US-18 mit dem CLI  
**Story Points:** 1  
**Abhängigkeit:** US-17, US-18  
**Package:** `cli/`, `orchestrator/`

### Ziel

```
ÄNDERN: src/main/java/org/example/cli/Main.java (neues Flag)
ÄNDERN: src/main/java/org/example/orchestrator/AnalysisOrchestrator.java (neue Methode)
```

### Spezifikation

```bash
# Nach dem Evaluieren aller 3 Projekte:
java -jar target/CodeSmellsDetector.jar --aggregate \
  --labels output/projectA/labeling_input.csv \
  --labels output/projectB/labeling_input.csv \
  --labels output/projectC/labeling_input.csv \
  --output output/aggregated
```

Oder einfacher (ein Verzeichnis mit Unterordnern):

```bash
java -jar target/CodeSmellsDetector.jar --aggregate \
  --output-root output \
  --output output/aggregated
```

Die Methode scannt `output/*/metrics_summary.csv` und aggregiert.

### Akzeptanzkriterien

```
□ CLI erkennt --aggregate Flag
□ Liest metrics_summary.csv aus allen Unterordnern
□ Erzeugt output/aggregated/aggregated_metrics.csv + aggregated_report.md
□ mvn test grün
```

---

## US-20: Sensitivitätsanalyse (Label-Threshold)

**Priorität:** P2 – Im Exposé als Gegenmaßnahme für Construct Validity versprochen  
**Story Points:** 2  
**Abhängigkeit:** Keine (aber sinnvoll erst nach Labeling)  
**Package:** `orchestrator/`, `cli/`

### Ziel

```
ÄNDERN: src/main/java/org/example/orchestrator/AnalysisOrchestrator.java
ÄNDERN: src/main/java/org/example/cli/Main.java
```

### Kontext

`LabelDTO.deriveLabel()` hat bereits drei Stufen:
- ≥ 3/4 Kriterien → GOD_CLASS
- 2/4 Kriterien → UNCERTAIN
- ≤ 1/4 Kriterien → NO

Die Sensitivitätsanalyse braucht zwei Durchläufe:
1. **Streng (4/4):** Nur GOD_CLASS, UNCERTAIN → NO
2. **Liberal (3/4):** GOD_CLASS, UNCERTAIN → GOD_CLASS (oder wie bisher ≥3)

### Spezifikation

Neues CLI-Flag:

```bash
java -jar target/CodeSmellsDetector.jar --evaluate \
  --labels output/labeling_input.csv \
  --label-threshold 4 \       # Streng: nur 4/4 = God Class
  --output output/sensitivity_strict

java -jar target/CodeSmellsDetector.jar --evaluate \
  --labels output/labeling_input.csv \
  --label-threshold 2 \       # Liberal: 2/4 = God Class (uncertain → positive)
  --output output/sensitivity_liberal
```

**Implementation:** In `AnalysisOrchestrator.evaluate()` den `buildGroundTruth()`-Schritt erweitern:

```java
// Statt nur GOD_CLASS als positiv:
private Map<String, Boolean> buildGroundTruth(Map<String, LabelDTO> labels, int threshold) {
    Map<String, Boolean> truth = new LinkedHashMap<>();
    for (Map.Entry<String, LabelDTO> entry : labels.entrySet()) {
        LabelDTO label = entry.getValue();
        int trueCount = countTrue(label.getK1()) + countTrue(label.getK2())
                       + countTrue(label.getK3()) + countTrue(label.getK4());
        truth.put(entry.getKey(), trueCount >= threshold);
    }
    return truth;
}
```

### Akzeptanzkriterien

```
□ --label-threshold akzeptiert Werte 1–4
□ Default bleibt 3 (wie bisher)
□ threshold=4 erzeugt weniger Ground-Truth-Positive (strengere Evaluation)
□ threshold=2 erzeugt mehr Ground-Truth-Positive
□ Alle Evaluation-Outputs werden korrekt erzeugt
□ mvn test grün
```

---

## US-21: Repo-Hygiene für Abgabe

**Priorität:** P2 – Kosmetisch, aber wichtig für Eindruck beim Betreuer  
**Story Points:** 1  
**Abhängigkeit:** Keine  
**Package:** Kein Java-Code

### Aufgaben

```
□ dependency-reduced-pom.xml löschen + in .gitignore aufnehmen
□ AGENTS.md, CLAUDE.md, PROMPTS.md, SKILL.md, WORKFLOW.md, REFERENCE.md
  → in .gitignore aufnehmen ODER in Branch "tooling" verschieben
□ .claude/ Verzeichnis aus Repo entfernen + in .gitignore
□ mnt/ Verzeichnis aus Repo entfernen (schon in .gitignore, aber im ZIP enthalten)
□ output/ Verzeichnis: sicherstellen, dass es wirklich nicht committed ist
□ Context_Code.md: "TODO-LINK-EINFUEGEN" → "https://github.com/Sekauf/CodeSmellsDetector"
□ README.md: Lizenz-Hinweis ergänzen (MIT oder Apache 2.0)
□ Optional: GitHub Actions CI (.github/workflows/ci.yml)
□ .idea/ komplett in .gitignore (spezifische Dateien schon drin, aber Verzeichnis noch teilweise getrackt)
```

### Akzeptanzkriterien

```
□ git status zeigt keine untracked/ignored Dateien die ins Repo gehören
□ git clone auf frischem Rechner → mvn clean test → grün
□ Keine Claude/Codex-spezifischen Dateien im main Branch sichtbar
□ README enthält: Zweck, Build-Anleitung, CLI-Usage, Lizenz
```

---

## Zeitplan-Empfehlung

```
Tag 1:  US-16 (Batch-Skript) + US-15 (Bootstrap-CIs)
Tag 2:  US-17 (Aggregation) + US-18 (Aggregierter Report)
Tag 3:  US-19 (CLI --aggregate) + US-20 (Sensitivitätsanalyse)
Tag 4:  US-21 (Repo-Hygiene) + Puffer/Bugfixes

Parallel zu Tag 1–4: Labeling der 79 JFreeChart-Kandidaten (manuell, ~4h)
Danach: 2 weitere Projekte analysieren + labeln
```

---

## Was diese Stories NICHT abdecken (bewusst)

**Baseline-Operationalisierung (WMC/TCC/ATFD):** Die leeren Klassen `GodClassRule` und `BaselineMetricsCalculator` werden bewusst NICHT als Story aufgenommen. Begründung:

1. TCC erfordert Data-Flow-Analyse (welche Methoden teilen welche Attribute) – das ist mit JavaParser machbar, aber ~500+ LOC und fehleranfällig.
2. ATFD erfordert vollständige Typauflösung (Zugriffe auf fremde Attribute) – JavaParser ohne Symbolsolver kann das nicht zuverlässig.
3. Der Zeitrahmen erlaubt es nicht mehr realistisch.

**Empfehlung für die Arbeit:** In Kapitel 5 (Implementierung) transparent beschreiben:
> „Der Baseline-Detektor verwendet als Größenmetrik die Summe aus Methoden- und Feldanzahl sowie die Anzahl referenzierter Typen. Diese vereinfachte Operationalisierung wurde gewählt, da eine robuste Berechnung von TCC und ATFD eine vollständige Typauflösung erfordert, die im Rahmen dieser Arbeit nicht realisierbar war. Die Abweichung von der im Exposé beschriebenen Marinescu-Detection-Strategy wird in Abschnitt 7.3 als Threat to Construct Validity diskutiert."
