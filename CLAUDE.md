# CLAUDE.md

## Projekt
God-Class Evaluierungstool — Bachelorarbeit. Java 17, Maven, CLI.

## Wichtigste Regel
**IMMER Plan-First:** Bevor du Code schreibst, nutze den `/plan-first` Skill.
Zeige betroffene Dateien, Methoden-Signaturen, Tests, Risiken. Warte auf Bestätigung.

## Geschützte Packages (NIEMALS ändern)
- `org.example.baseline/` — BaselineAnalyzer, CandidateDTO, MetricsCalculator etc.
- `org.example.sonar/` — SonarAnalyzer, IssuesClient etc.
- `org.example.jdeodorant/` — JDeodorantImporter etc.
- `org.example.sampling/` — SamplingEngine

## Neue Packages (hier arbeiten)
- `org.example.labeling/` — LabelDTO, CSV-Import/Export, Ground Truth
- `org.example.evaluation/` — ToolEvaluator, Agreement, FP/FN
- `org.example.reporting/` — ReportGenerator

## Regeln
- `mvn test` muss IMMER grün sein
- Max 5 Dateien pro Änderung
- Jeder Code braucht Unit-Tests
- Bestehende Utilities wiederverwenden (siehe REFERENCE.md)
- Clean Code: Klassen ≤200 Zeilen, Methoden ≤30 Zeilen, Javadoc auf public
- CSV-Exports alphabetisch sortiert, deterministische Outputs

## Build
```bash
mvn test          # Tests
mvn package       # Build
```

## Weitere Doku
- `AGENTS.md` — Story-Tracker + Package-Status
- `REFERENCE.md` — Klassen-Referenz + Utilities
- `PROMPTS.md` — Fertige Prompts pro Story
- `WORKFLOW.md` — Schritt-für-Schritt-Anleitung
