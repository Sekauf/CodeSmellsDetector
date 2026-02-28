# PROMPTS.md — Fertige Prompts pro User Story

Kopiere den jeweiligen Block und paste ihn in Codex/Claude CLI.

---

## US-01 — LabelDTO + Labeling-CSV-Export

```
Ziel: Neues Package org.example.labeling mit LabelDTO und LabelCsvExporter.

Kontext:
- CandidateDTO in org.example.baseline (NICHT ändern)
- CandidateCsvUtil.escapeCsvField() wiederverwenden
- ResultExporter.stableSorting() wiederverwenden
- Siehe REFERENCE.md für Details

Constraints: 2 neue Dateien + 1 Testdatei. Kein Umbau bestehender Klassen.

Akzeptanz:
- LabelDTO: fullyQualifiedClassName, k1–k4 (Boolean), comment (String), finalLabel (enum GOD_CLASS/UNCERTAIN/NO)
- LabelDTO.deriveLabel(k1,k2,k3,k4): ≥3 true → GOD_CLASS, 2 → UNCERTAIN, sonst NO
- LabelCsvExporter.export(List<CandidateDTO>, Path): CSV mit leeren Label-Spalten
- Header: fullyQualifiedClassName,baselineFlag,sonarFlag,jdeodorantFlag,methodCount,fieldCount,dependencyTypeCount,k1,k2,k3,k4,comment,finalLabel
- Tests: deriveLabel alle Kombinationen + CSV mit 3 Mock-Kandidaten
- mvn test grün
```

## US-02 — Labeling-CSV-Import

```
Ziel: LabelCsvImporter in org.example.labeling.

Kontext:
- LabelDTO aus US-01
- CandidateCsvUtil.parseCsvLine() wiederverwenden
- k1–k4 Werte: ja/nein/true/false/1/0 (case-insensitive)

Constraints: 1 neue Datei + 1 Testdatei.

Akzeptanz:
- import(Path) → Map<String, LabelDTO>
- finalLabel leer → auto-derive via LabelDTO.deriveLabel()
- finalLabel gesetzt → Override übernehmen
- Warnung bei unbekannten Werten, kein Crash
- Tests: valide CSV, Override, fehlerhafte Zeilen
- mvn test grün
```

## US-03 — Blind-Negativ-Samples einbinden

```
Ziel: AnalysisOrchestrator erweitern — Blind-Samples + Labeling-CSV erzeugen.

Kontext:
- SamplingEngine.sampleBlindNegativesTopPercentile() existiert
- LabelCsvExporter aus US-01
- AnalysisOrchestrator.run() erweitern (am Ende)

Constraints: NUR AnalysisOrchestrator.java ändern + 1 Testdatei.

Akzeptanz:
- Blind-Samples (seed=42, count=5, percentile=0.1) zu Merged-Liste
- labeling_input.csv wird nach results.csv erzeugt
- Test: Mocked Runners → labeling_input.csv enthält Blind-Samples
- mvn test grün
```

## US-04 — Ground-Truth-Export

```
Ziel: GroundTruthExporter in org.example.labeling.

Kontext:
- LabelDTO + LabelCsvImporter aus US-01/02
- Jackson ObjectMapper (bereits in pom.xml)
- CandidateCsvUtil.escapeCsvField()

Constraints: 1 neue Datei + 1 Testdatei.

Akzeptanz:
- export(Map<String,LabelDTO>, Path) → ground_truth.csv + ground_truth.json
- CSV: FQCN, k1, k2, k3, k4, comment, finalLabel
- JSON: { metadata: {total, godClassCount, noCount, uncertainCount, date}, labels: [...] }
- Test: Mock-Labels → erwarteter CSV/JSON-Inhalt
- mvn test grün
```

## US-05 — Zweitbewertung + Reliabilität

```
Ziel: SecondReviewExporter, SecondReviewEvaluator in org.example.labeling.

Kontext:
- SamplingEngine.sampleSecondReview() existiert
- ReliabilityEvaluator.computeReliability() existiert
- LabelDTO, LabelCsvImporter, LabelCsvExporter wiederverwenden

Constraints: 2 neue Dateien + 1 Testdatei.

Akzeptanz:
- SecondReviewExporter: 20%-Stichprobe als blinde CSV (ohne Primary-Labels)
- SecondReviewEvaluator: Primary + Secondary Labels → ReliabilityMetrics + Konflikt-CSV
- Konflikt-CSV: FQCN, primaryLabel, secondaryLabel, resolvedLabel (leer)
- Reliability als JSON (agreement, kappa, ac1)
- Test: 10 Mock-Labels, 2 Konflikte → korrekte Metriken
- mvn test grün
```

## US-06 — Specificity

```
Ziel: EvaluationMetrics + MetricsEngine um specificity erweitern.

Kontext: org.example.metrics — bestehende Klassen.

Constraints: NUR EvaluationMetrics.java + MetricsEngine.java ändern. Bestehende Tests nicht brechen.

Akzeptanz:
- specificity = TN / (TN+FP), div-by-zero → 0.0
- Bestehende Tests grün
- 1 neuer Test
- mvn test grün
```

## US-07 — Tool-Evaluation gegen Ground Truth

```
Ziel: ToolEvaluator in org.example.evaluation.

Kontext:
- MetricsEngine.computeMetrics(Set<String>, Set<String>, int) existiert
- Jackson für JSON

Constraints: 1 neue Datei + 1 Testdatei.

Akzeptanz:
- evaluate(Map<FQCN,Boolean> gt, Set<FQCN> preds, int total) → EvaluationMetrics
- evaluateAll(gt, baselinePreds, sonarPreds, jdeoPreds, total) → Map<String, EvaluationMetrics>
- Export: evaluation_per_tool.json
- Test: bekannte Daten → erwartete Metriken
- mvn test grün
```

## US-08 — Tool-Agreement

```
Ziel: ToolAgreementCalculator + OverlapResult in org.example.evaluation.

Constraints: 2 neue Dateien + 1 Testdatei.

Akzeptanz:
- jaccard(Set a, Set b) → double = |A∩B| / |A∪B|
- computeAll(baseline, sonar, jdeo) → List<OverlapResult>
- OverlapResult: toolA, toolB, jaccard, both, onlyA, onlyB
- Export: tool_agreement.csv
- Test: bekannte Sets → erwarteter Jaccard
- mvn test grün
```

## US-09 — FP/FN-Listen

```
Ziel: ErrorAnalysisExporter in org.example.evaluation.

Constraints: 1 neue Datei + 1 Testdatei.

Akzeptanz:
- Pro Tool: CSV mit FQCN, errorType (FP/FN), methodCount, fieldCount, dependencyTypeCount, Flags
- Sortiert: errorType dann FQCN
- Test: Mock-Daten → erwartete Zeilen
- mvn test grün
```

## US-10 — Metrics-Summary CSV

```
Ziel: writeMetricsSummaryCsv() in ResultExporter oder neue Klasse in org.example.reporting.

Constraints: Max 1 geänderte oder 1 neue Datei + 1 Testdatei.

Akzeptanz:
- CSV: tool,precision,recall,f1,specificity,mcc,tp,fp,fn,tn
- Eine Zeile pro Tool, sortiert: baseline, jdeodorant, sonarqube
- Test + mvn test grün
```

## US-11 — Markdown-Report

```
Ziel: ReportGenerator in org.example.reporting.

Constraints: 1 neue Datei + 1 Testdatei.

Akzeptanz:
- report.md: Metadaten, Confusion Matrix pro Tool, Metriken-Tabelle, Agreement, Reliability, Statistik
- Alles Markdown-Tabellen, deterministisch
- Test: Mock-Daten → Report enthält erwartete Strings
- mvn test grün
```

## US-12 — CLI-Erweiterung

```
Ziel: Main.java + AnalysisOrchestrator um --evaluate Modus erweitern.

Constraints: NUR Main.java + AnalysisOrchestrator ändern.

Akzeptanz:
- --analyze: wie bisher + labeling_input.csv
- --evaluate --labels <csv>: Labels importieren → Metriken + Agreement + FP/FN + Report
- --evaluate --labels <csv> --second-review-labels <csv>: zusätzlich Reliability
- Fehler → Log + weiter (kein Totalabbruch)
- mvn test grün
```

## US-13 — E2E Integration Test

```
Ziel: PipelineIntegrationTest.

Constraints: 1 Testdatei + Fixtures.

Akzeptanz:
- Golden-Projekt → Baseline → Mock-Sonar → Mock-JDeo → Merge → Labels → Metrics → Report
- Alle Output-Dateien existieren, JSON valid, Metriken plausibel
- Kein Docker, < 10s
- mvn test grün
```

## US-14 — README

```
Ziel: README.md aktualisieren.

Akzeptanz:
- Zweck, Voraussetzungen, Build, CLI-Usage mit Beispielen
- Pipeline erklärt (Analyse → Labeling → Evaluation)
- CSV-Formate dokumentiert
- Reproduzierbarkeitshinweise (Seed, Versionen)
```
