# REFERENCE.md — Klassen-Referenz & Utilities

Lese diese Datei wenn du wissen musst welche Klassen existieren oder welche Utilities du wiederverwenden sollst.

## Wiederverwendbare Utilities (IMMER nutzen statt neu bauen)

| Klasse | Methode | Nutzen für |
|--------|---------|-----------|
| `baseline.CandidateCsvUtil` | `escapeCsvField(String)` | Jeden CSV-Export |
| `baseline.CandidateCsvUtil` | `parseCsvLine(String)` | Jeden CSV-Import |
| `export.ResultExporter` | `stableSorting(List<CandidateDTO>)` | Deterministische Sortierung |
| `export.ResultExporter` | `createOutputDir(Path)` | Output-Verzeichnis anlegen |
| `sampling.SamplingEngine` | `sampleBlindNegativesTopPercentile(...)` | Blind-Negativ-Samples |
| `sampling.SamplingEngine` | `sampleSecondReview(...)` | Zweitbewertungs-Stichprobe |
| `metrics.ReliabilityEvaluator` | `computeReliability(List, List)` | Kappa/AC1 |
| `metrics.MetricsEngine` | `computeMetrics(Set, Set, int)` | P/R/F1/MCC |

## CandidateDTO (zentral, NICHT ändern)

```java
// Package: org.example.baseline
CandidateDTO {
    String fullyQualifiedClassName    // Primärschlüssel
    boolean baselineFlag, sonarFlag, jdeodorantFlag
    Integer wmcNullable, Double tccNullable, Integer atfdNullable, Integer cboNullable, Integer locNullable
    int methodCount, fieldCount, dependencyTypeCount
    List<String> reasons
    boolean godClass, usedCboFallback
}
// Builder: CandidateDTO.builder("com.example.Foo").sonarFlag(true).build()
```

## Alle Klassen nach Package

### baseline/ (✅ fertig, nicht anfassen)
- `BaselineAnalyzer` — Hauptklasse, erkennt God Classes (methodCount+fieldCount > threshold ODER dependencyTypes > threshold)
- `BaselineThresholds` — Schwellenwerte (default: methods+fields>40, deps>5)
- `MetricsCalculator` — berechnet methodCount, fieldCount, dependencyTypeCount aus Source
- `ClassMetrics` — DTO: fqn, methodCount, fieldCount, dependencyTypeCount
- `CandidateDTO` — Zentrale DTO für alle Packages
- `CandidateCsvUtil` — CSV-Escaping + Parsing
- `BaselineCandidateExporter` / `BaselineCandidateImporter` — CSV I/O
- `SourceScanner` — findet .java-Dateien (filtert test/generated)
- `JavaSourceParser` — parst zu ParsedType
- `ParsedType` — Parsed-Klasse mit FQCN + Source-File
- `AnalysisMetadata` — Java/JavaParser-Version + Timestamp
- `BaselineAnalysisResult` — Candidates + Metadata Wrapper
- `BaselineMetricsCalculator` — Zusätzliche Metriken
- `GodClassRule` — Leerer Stub (nicht nutzen)

### sonar/ (✅ fertig, nicht anfassen)
- `SonarAnalyzer` — Orchestriert Docker → Scan → Issues → Map
- `SonarConfig` — Host, Token, ProjectKey, Docker-Settings
- `SonarDockerManager` — Start/Stop via docker-compose
- `SonarHealthClient` — Wartet auf Ready
- `SonarScannerRunner` — mvn sonar:sonar ausführen
- `SonarScanResult` — exitCode + output + ceTaskId
- `CeTaskClient` — CE-Task-Status pollen
- `SonarIssuesClient` — REST-API Issues (paginiert)
- `SonarIssue` — DTO: component, rule, message, line
- `SonarS6539Mapper` — SonarIssue → CandidateDTO

### jdeodorant/ (✅ fertig, nicht anfassen)
- `JDeodorantImporter` — CSV-Import (flexible Header, auto-Delimiter)
- `JDeodorantIntegration` — Headless oder CSV-Fallback
- `ProjectConfig` — CSV-Pfad + Headless-Flag

### metrics/ (✅ fertig, nur Specificity ergänzen)
- `MetricsEngine` — Confusion Matrix → P/R/F1/MCC
- `EvaluationMetrics` — DTO: tp, fp, fn, tn, precision, recall, f1, mcc
- `ReliabilityEvaluator` — Agreement%, Kappa, AC1
- `ReliabilityMetrics` — DTO: counts + agreement + kappa + ac1

### sampling/ (✅ fertig, nicht anfassen)
- `SamplingEngine` — sampleBalanced(), sampleSecondReview(), sampleBlindNegativesTopPercentile()

### export/ (✅ fertig, erweitern erlaubt)
- `ResultExporter` — writeCsv(), writeJson(), writeMetricsJson(), stableSorting()

### orchestrator/ (🔧 erweitern)
- `AnalysisOrchestrator` — run(): Baseline+Sonar+JDeo → mergeCandidates() → Export

### cli/ (🔧 erweitern)
- `Main` — CLI mit --project, --output, --run-sonar, --jdeodorant-csv, --help

### logging/ (✅ fertig)
- `LoggingConfigurator` — File + Console Logging

## Bestehende Tests (82 @Test)

29 Testdateien in baseline/, sonar/, jdeodorant/, metrics/, sampling/, export/, golden/, logging/. Golden-Mini-Projekte in `src/test/resources/golden/` und `src/test/resources/mini-project/`.
