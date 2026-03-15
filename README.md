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
  - laufender SonarQube-Server (Default: `http://localhost:9000`)
  - optional `SONAR_TOKEN`
  - optional `SONAR_DOCKER_ENABLED=true`
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

Beispiel mit Second Review:

```bash
java -cp target/classes org.example.cli.Main \
  --evaluate \
  --labels output/labeling_input.csv \
  --second-review-labels output/second_review.csv \
  --output output
```

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
