# AGENTS.md — God-Class Evaluierungstool

## Projekt

Bachelorarbeit: Vergleich von 3 God-Class-Detektoren (SonarQube, JDeodorant, eigener Baseline) auf Java-OSS-Projekten. CLI-Tool, kein Web-UI. Evaluierungs-Toolkit, kein Produktivsystem.

## Wichtigste Regel

**IMMER Plan-First:** Bevor du Code schreibst, nutze den `$plan-first` Skill.
Zeige betroffene Dateien, Methoden-Signaturen, Tests, Risiken. Warte auf Bestätigung.

## Story-Tracker (nach jeder Story updaten!)

### Phase 1: CLI & Evaluierungs-Pipeline

| Story | Status | Beschreibung |
|-------|--------|-------------|
| US-01 | ✅ | LabelDTO + Labeling-CSV-Export |
| US-02 | ✅ | Labeling-CSV-Import |
| US-03 | ✅ | Blind-Negativ-Samples einbinden |
| US-04 | ✅ | Ground-Truth-Export |
| US-05 | ✅ | Zweitbewertung + Reliabilität |
| US-06 | ✅ | Specificity in EvaluationMetrics |
| US-07 | ✅ | Tool-Evaluation gegen Ground Truth |
| US-08 | ✅ | Tool-Agreement (Jaccard) |
| US-09 | ✅ | FP/FN-Listen pro Tool |
| US-10 | ✅ | Metrics-Summary CSV |
| US-11 | ✅ | Markdown-Report |
| US-12 | ✅ | CLI-Erweiterung (--evaluate) |
| US-13 | ⬜ | E2E Integration Test |

### Phase 2: GUI-Redesign & One-Click-Analyse

| Story | Priorität | SP | Status | Beschreibung |
|-------|-----------|-----|--------|-------------|
| S-01 | Must-Have | 3 | ✅ | Projektverzeichnis auswählen (FileChooser + D&D + Verlauf) |
| S-02 | Must-Have | 5 | ✅ | Tool-Konfiguration übersichtlich darstellen (TabbedPane) |
| S-03 | Should-Have | 2 | ✅ | Output-Verzeichnis und Projektname konfigurieren |
| S-04 | Must-Have | 5 | ✅ | One-Click-Analyse starten (SwingWorker) |
| S-05 | Should-Have | 3 | ✅ | Fortschrittsanzeige während Analyse (ProgressBar + Log) |
| S-06 | Could-Have | 3 | ✅ | SonarQube-Docker automatisch starten/stoppen |
| S-07 | Should-Have | 2 | ✅ | Analyse abbrechen (cancel + Cleanup) |
| S-08 | Must-Have | 5 | ✅ | Kandidaten-Tabelle anzeigen (JTable + Sorter + Farben) |
| S-09 | Should-Have | 3 | ✅ | Tabelle filtern und durchsuchen (CompositeRowFilter) |
| S-10 | Should-Have | 5 | ✅ | Tool-Agreement-Übersicht anzeigen (Jaccard + Venn) |
| S-11 | Must-Have | 3 | ✅ | Ergebnisse exportieren (CSV/JSON/Markdown per Dialog) |
| S-12 | Must-Have | 8 | ⬜ | Inline-Labeling in der Tabelle (K1–K4, finalLabel) |
| S-13 | Must-Have | 5 | ⬜ | Evaluation aus GUI starten (P/R/F1, FP/FN, Reliabilität) |


## Randbedingungen

Java 17, Maven, JUnit 5+4, Jackson 2.17.2. `mvn test` muss **immer** grün sein.

## Package-Status

| Package | Status | Anfassen? |
|---------|--------|-----------|
| `baseline/` | ✅ fertig | **NEIN** |
| `sonar/` | ✅ fertig | **NEIN** |
| `jdeodorant/` | ✅ fertig | **NEIN** |
| `metrics/` | ✅ fertig | NUR Specificity (US-06) |
| `sampling/` | ✅ fertig | **NEIN** |
| `export/` | ✅ fertig | Erweitern erlaubt |
| `orchestrator/` | 🔧 | Erweitern (US-03, US-12) |
| `cli/` | 🔧 | Erweitern (US-12) |
| `labeling/` | ❌ NEU | US-01 bis US-05 |
| `evaluation/` | ❌ NEU | US-07 bis US-09 |
| `reporting/` | ❌ NEU | US-11 |

Detaillierte Klassen-Referenz + wiederverwendbare Utilities → siehe **REFERENCE.md**

## Strikte Regeln

### NIEMALS
- Fertige Packages (baseline, sonar, jdeodorant, metrics, sampling) umbauen
- CandidateDTO ändern
- Bestehende Tests brechen oder löschen
- Neue Maven-Dependencies ohne Begründung
- Mehr als **5 Dateien** pro Prompt ändern/erstellen
- Code ohne zugehörige Unit-Tests
- Dateien ändern die nicht explizit im Prompt genannt sind

### IMMER
- `mvn test` grün nach jeder Änderung
- Utilities wiederverwenden (→ REFERENCE.md)
- Logging: Start/Ende + Counts
- CSV-Exports alphabetisch nach FQCN sortiert
- Null/Leer defensiv behandeln (Warnung, kein Crash)

### Clean Code (in jeder Datei)
- Klassen ≤ 200 Zeilen, Methoden ≤ 30 Zeilen — wenn länger, aufteilen
- Jede Klasse eine Verantwortung (SRP)
- Keine Magic Numbers — Konstanten mit sprechendem Namen (`private static final`)
- Keine verschachtelten if/else > 2 Ebenen — early return nutzen
- Methodennamen = Verb + Beschreibung (`exportLabelsAsCsv`, nicht `doExport`)
- Keine auskommentierten Code-Blöcke
- Javadoc auf jeder public Methode (1–2 Sätze reichen)
- `final` auf Felder wo möglich
- Keine Raw-Types (immer `List<String>`, nie `List`)

## Prompt-Template

```
Ziel: [Klassen/Dateien die entstehen sollen]
Kontext: [Existierende Klassen — siehe REFERENCE.md]
Constraints: Max [N] Dateien. Keine Änderungen an [Packages].
Akzeptanz: [Tests + Assertions]. mvn test grün.
```

Fertige Prompts pro Story → siehe **PROMPTS.md**

## Tripwires (sofort stoppen wenn)

- `mvn test` rot
- Agent hat fertige Packages umstrukturiert
- Bestehende Tests geändert statt neuen Code zu fixen

## Workflow

Schritt-für-Schritt-Anleitung pro Story → siehe **WORKFLOW.md**
