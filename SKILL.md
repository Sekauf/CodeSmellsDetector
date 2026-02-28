---
name: plan-first
description: >
  ALWAYS use this skill before writing any code. Triggers on ALL implementation
  requests, feature work, bug fixes, refactoring, or any task that changes files.
  Creates a plan first, waits for approval, then implements.
---

# Plan-First Workflow

## Before ANY code change, output this plan:

### 1. Betroffene Dateien
List JEDE Datei die erstellt oder geändert wird. Format:
- `NEU: src/main/java/org/example/labeling/LabelDTO.java`
- `ÄNDERN: src/main/java/org/example/orchestrator/AnalysisOrchestrator.java`

### 2. Verbotene Dateien — Prüfe ob du diese anfasst:
- ❌ baseline/ (außer explizit erlaubt)
- ❌ sonar/
- ❌ jdeodorant/
- ❌ metrics/ (außer explizit erlaubt)
- ❌ sampling/

Wenn ja → Plan anpassen bis keine verbotene Datei mehr drin ist.

### 3. Methoden-Signaturen
Für jede neue public Methode: Name, Parameter, Rückgabetyp, 1-Satz-Beschreibung.

### 4. Tests
Welche Testklassen/Testmethoden werden erstellt? Was wird assertet?

### 5. Risiken
Was könnte schiefgehen? Bricht das bestehende Tests?

## Dann STOPP und frage:
> "Plan steht. Soll ich implementieren?"

## Erst nach Bestätigung: Code schreiben.
