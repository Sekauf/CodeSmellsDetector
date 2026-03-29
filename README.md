# CodeSmellsDetector

CLI-Toolkit zur Evaluierung von God-Class-Detektoren auf Java-Projekten. Das Tool kombiniert drei Erkennungsquellen (Baseline, SonarQube, JDeodorant), erzeugt Labeling-Artefakte und berechnet Evaluationsmetriken gegen Ground Truth.

## Zweck

- Kandidatenklassen aus mehreren Detektoren zusammenfuehren
- Labeling-CSV fuer manuelle Annotation erzeugen
- Tool-Performance gegen annotierte Labels auswerten (Precision, Recall, F1, Specificity, MCC)
- Tool-Agreement (Jaccard), FP/FN-Listen und Markdown-Report erzeugen

## Voraussetzungen

- Java 17
- Maven 3.9+
- Optional fuer SonarQube-Lauf:

  ### SonarQube-Setup

  1. Docker-Compose starten:
     ```bash
     docker compose up -d
     ```
  2. Warten bis SonarQube bereit ist:
     ```bash
     ./scripts/sonar_wait_ready.sh
     # Windows:
     ./scripts/sonar_wait_ready.ps1
     ```
  3. `SONAR_TOKEN` als Umgebungsvariable setzen:
     ```bash
     export SONAR_TOKEN=<token>
     ```
     Hinweis: Token **nicht** direkt im Code hardcodieren.
- Optional fuer JDeodorant:
  - vorhandener JDeodorant-CSV-Export

Projektversionen (aus `pom.xml`):

- Jackson Databind `2.17.2`
- JUnit Jupiter/Vintage `5.10.2`
- JUnit 4 `4.13.2`

## Build

```bash
mvn clean test
mvn package
```

Hinweis: Die CLI kann direkt ueber die Main-Klasse gestartet werden. Das ist robuster als ein fixer Jar-Dateiname.

```bash
java -cp target/classes org.example.cli.Main --help
```

## GUI starten

```bash
java -jar target/CodeSmellsDetector-gui.jar
```

Features:

- Dreistufiger Workflow: SETUP → PROGRESS → RESULTS
- One-Click-Analyse (Baseline, SonarQube, JDeodorant konfigurierbar)
- Inline-Labeling direkt in der Ergebnistabelle (K1-K4, FinalLabel)
- Evaluation und Reliabilitaetsanalyse per Knopfdruck
- Export-Dialog fuer CSV/JSON/Markdown-Report
- Venn-Diagramm der Tool-Uebereinstimmung

## CLI Usage

### Analyse-Modus

```bash
java -cp target/classes org.example.cli.Main \
  --project C:/pfad/zum/java-projekt \
  --output output
```

Optionen:

- `--project <path>` Quellprojekt
- `--output <dir>` Ausgabeverzeichnis (Default: `output/`)
- `--run-sonar` SonarQube-Analyse aktivieren
- `--jdeodorant-csv <path>` JDeodorant CSV importieren
- `--baseline-methods-fields <n>` Baseline-Schwelle (Default: `40`)
- `--baseline-dependency-types <n>` Baseline-Schwelle (Default: `5`)

Beispiel mit allen Quellen:

```bash
java -cp target/classes org.example.cli.Main \
  --project C:/pfad/zum/java-projekt \
  --run-sonar \
  --jdeodorant-csv C:/daten/jdeodorant.csv \
  --output output
```

### Evaluate-Modus

```bash
java -cp target/classes org.example.cli.Main \
  --evaluate \
  --labels output/labeling_input.csv \
  --output output
```

Optionen:

- `--evaluate` Evaluationsmodus
- `--labels <csv>` annotierte Labeling-CSV (Pflicht)
- `--second-review-labels <csv>` zweite Bewertung fuer Reliabilitaet (optional)
- `--output <dir>` Ausgabeverzeichnis (Default: `output/`)
- `--label-threshold <n>` Schwellenwert fuer positive Labels (1-4, Default: `3`)

Beispiel mit Second Review:

```bash
java -cp target/classes org.example.cli.Main \
  --evaluate \
  --labels output/labeling_input.csv \
  --second-review-labels output/second_review.csv \
  --output output
```

### Aggregate-Modus

```bash
java -cp target/classes org.example.cli.Main \
  --aggregate \
  --output-root output/ \
  --output output/aggregated
```

Optionen:

- `--aggregate` Multi-Projekt-Aggregation aktivieren
- `--output-root <dir>` Wurzelverzeichnis mit Projektunterordnern (Pflicht)
- `--output <dir>` Ausgabeverzeichnis fuer aggregierte Ergebnisse

## Batch-Modus

```bash
./scripts/run_batch.sh
```

- Fuehrt die Analyse fuer mehrere Projekte sequenziell aus
- Konfiguration via Variablen `JAR`, `OUTPUT_BASE` sowie `PROJECTS`/`PATHS`/`JD_CSVS`-Arrays im Skript
- Jedes Projekt erhaelt ein eigenes Unterverzeichnis unter `OUTPUT_BASE`

## Projektstruktur

| Package | Beschreibung |
|---------|-------------|
| `baseline` | God-Class-Erkennung per Metrik-Schwellenwerte |
| `sonar` | SonarQube-Integration (Scanner, API, Mapper) |
| `jdeodorant` | Import von JDeodorant-CSV-Exporten |
| `evaluation` | Metriken (Precision, Recall, F1, MCC, Reliability) |
| `gui` | Swing-GUI (28 Klassen, SETUP-PROGRESS-RESULTS) |
| `cli` | Argument-Parsing und Einstiegspunkt |
| `orchestrator` | Koordination der Analyse-Pipeline |
| `export` | CSV/JSON-Export |
| `reporting` | Markdown-Report-Generierung |
| `labeling` | Labeling-Artefakte und Persistenz |
| `sampling` | Blind-Negative-Sampling (Seed 42) |
| `metrics` | Metrik-Berechnungen |
| `logging` | Logging-Konfiguration |

## Pipeline

1. Analyse
- Baseline, SonarQube und JDeodorant liefern Kandidaten.
- Ergebnisse werden zusammengefuehrt und exportiert:
  - `results.csv`
  - `results.json`
  - `labeling_input.csv` (fuer manuelle Annotation)

2. Labeling
- `labeling_input.csv` wird manuell befuellt (`k1..k4`, `comment`, `finalLabel`).
- Optional: zweite blinde Bewertung fuer Reliabilitaet.

3. Evaluation
- Annotierte Labels werden importiert und als Ground Truth verwendet.
- Outputs:
  - `evaluation_per_tool.json`
  - `tool_agreement.csv`
  - `fp_fn_baseline.csv`, `fp_fn_sonar.csv`, `fp_fn_jdeodorant.csv`
  - `metrics_summary.csv`
  - `report.md`
  - optional bei Second Review: `second_review_conflicts.csv`, `reliability.json`

## CSV-Formate

### `labeling_input.csv` (Analyse-Output, Eingabe fuer Labeling/Evaluation)

Header:

```text
fullyQualifiedClassName,baselineFlag,sonarFlag,jdeodorantFlag,methodCount,fieldCount,dependencyTypeCount,k1,k2,k3,k4,comment,finalLabel
```

- Die Spalten `k1..k4`, `comment`, `finalLabel` sind initial leer und werden manuell gepflegt.
- `--labels` erwartet mindestens: `fullyQualifiedClassName,k1,k2,k3,k4,comment,finalLabel`.

### `metrics_summary.csv`

Header:

```text
tool,precision,recall,f1,specificity,mcc,tp,fp,fn,tn
```

### `tool_agreement.csv`

Header:

```text
toolA,toolB,jaccard,both,onlyA,onlyB
```

### `fp_fn_<tool>.csv`

Header:

```text
fqcn,errorType,methodCount,fieldCount,dependencyTypeCount,flags
```

## Reproduzierbarkeit

- Deterministische Sortierung in CSV-Exports (alphabetisch nach FQCN).
- Sampling fuer Blind-Negatives nutzt festen Seed `42` in der Pipeline.
- Gleiche Eingaben + gleiche Tool-/Umgebungs-Versionen erzeugen reproduzierbare Artefakte.
- Empfehlung: Java-/Maven-Versionen und externe Tool-Versionen (SonarQube, JDeodorant-Exportquelle) pro Run dokumentieren.

## Lizenz

Dieses Projekt ist unter der MIT-Lizenz lizenziert. Siehe [LICENSE](LICENSE) fuer Details.
