# Agent.md — God-Class Evaluierungstool (Java / IntelliJ / Codex CLI)

## Rolle
Du bist der Projekt-Agent für ein **Evaluierungstool zur God-Class-Erkennung** in Java. Du unterstützt bei:
- Architektur, Implementierung, Tests, Refactoring, Doku
- Integration von **SonarQube**, **JDeodorant** und einem **Baseline-Detektor** (JavaParser)
- Reproduzierbaren Experimenten inkl. Ground Truth (manuelles Labeling) und Metrik-Auswertung

**Wichtig:** Dieses Projekt ist ein Evaluierungs-Toolkit (Forschungs-/Studiencharakter), kein produktiver Dauerbetrieb.

---

## Projektziel (kurz)
Vergleiche die God-Class-Erkennungsleistung von **3 Detektoren** (SonarQube, JDeodorant, Baseline) auf **mind. 3 Java-OSS-Projekten** und liefere:
- eine **transparente Ground Truth**
- **Precision / Recall / F1** (plus optional Specificity, MCC)
- **Tool-Übereinstimmung** (z.B. Jaccard, ggf. Kappa/AC1 auf Tool-Labels)
- Exporte (CSV/JSON/Markdown-Report)

---

## Nicht-Ziele
- Kein “besserer” neuer Detektor; Baseline ist absichtlich simpel und transparent.
- Keine Anpassung/Kalibrierung von Tool-Schwellen (Sonar/JDeodorant laufen **out-of-the-box**).
- Fokus ausschließlich auf **God Class / Blob / Monster Class**.

---

## Feste Randbedingungen (Versionen & Umgebung)
- **Java:** 17+ (LTS)
- **SonarQube:** 2025.4 LTS (Docker)
- **JDeodorant:** 5.0.85
- **JavaParser:** 3.27.1
- **Build:** Maven bevorzugt (Gradle möglich)
- **Dev:** IntelliJ IDEA Ultimate
- **Container:** Docker (für SonarQube)

---

## Domänenwissen: God Class & Metriken

### God Class (Intuition)
Eine Klasse mit zu vielen Verantwortlichkeiten, niedriger Kohäsion, hoher Kopplung → Wartungsrisiko.

### Baseline-Metriken (pro Klasse)
- **WMC (Weighted Methods per Class):** Summe zyklomatischer Komplexität aller Methoden
- **TCC (Tight Class Cohesion):** Anteil von Methodenpaaren mit gemeinsamer Attributnutzung (0..1)
- **ATFD (Access to Foreign Data):** Zugriffe auf fremde Daten (andere Klassen)
  - Falls ATFD nicht robust: **CBO (Coupling Between Objects)** als Ersatz
- **LOC:** Zeilenanzahl

### Baseline-Detektionsregel (fix, nicht kalibrieren)
Markiere Klasse als God Class, wenn **alle** Bedingungen gelten:
1) **WMC ≥ 47**
2) **TCC < 0.33**
3) **ATFD > 5** *oder* (Fallback) **CBO ≥ 20**

Logge explizit, wenn CBO als Ersatz für ATFD genutzt wird (Threat to Construct Validity).

### SonarQube-Quelle
- Nutze Standard-Java-Regelwerk, insbesondere **java:S6539 "Monster Class"**
- Keine Custom Rules / kein Threshold-Tuning

### JDeodorant-Quelle
- Ermittele Klassen mit Extract-Class-Opportunities (als God-Class-Kandidaten)
- Headless/Automatisierung bevorzugt; falls nicht möglich: klarer Fallback-Pfad (manueller Export + Import)

---

## Pipeline-Logik (end-to-end)

### 1) Projekte einlesen & optional bauen
- Mindestens 3 Projekte (20–80 kLOC), produktiver Code; Test-/generierter Code filtern (Folder-Regeln).
- Optional Build (Maven/Gradle) → Fehler pro Projekt protokollieren, Gesamtprozess nicht stoppen.

### 2) Analyse
Für jedes Projekt:
- SonarQube ausführen → Kandidatenliste extrahieren (API/Report)
- JDeodorant ausführen → Kandidatenliste extrahieren
- Baseline (JavaParser) → Kandidatenliste berechnen

### 3) Kandidatenliste (Ground Truth Input)
- **Union** aller Klassen, die von mindestens einem Tool gemeldet wurden
- **+ Extra Negativ-Samples** (Blindtest): zufällig aus “Top-X% LOC/WMC”, die von keinem Tool gemeldet wurden
- Fixen Random-Seed nutzen → Reproduzierbarkeit

### 4) Manuelles Labeling (Ground Truth)
Für jede Kandidatenklasse: K1–K4 jeweils Ja/Nein + optional Kommentar
- **K1:** Disproportional groß/komplex (z.B. Top 5%)
- **K2:** Niedrige Kohäsion (erkennbar mehrere thematische Cluster)
- **K3:** Hohe Kopplung / viele Collaborators
- **K4:** Sinnvolle Extract-Class-Refaktorierung möglich (≥2 plausible Klassen)

Vorläufiges Label:
- **God Class = Ja**, wenn **≥ 3/4** Kriterien Ja
- **Unsicher**, wenn **2/4**
- sonst Nein

### 5) Zweitbewertung & Reliabilität
- 20% Stichprobe (stratifiziert), blind vom Secondary Reviewer
- Kennzahlen: percent agreement, **Cohen’s Kappa**, **Gwet’s AC1**
- Konflikte exportierbar/übersichtlich; finales Label nach externer Klärung editierbar
- Sonderfall: 100% Agreement → Kappa-Berechnung darf nicht crashen (Varianz=0)

### 6) Auswertung
- Confusion Matrix je Tool + Metriken:
  - Precision, Recall, F1 (pro Projekt + aggregiert)
  - optional Specificity, MCC
- Tool-Übereinstimmung:
  - Jaccard je Tool-Paar
  - optional Cohen’s Kappa / AC1 auf “Tool meldet/ meldet nicht” pro Klasse
- Qualitative Fehleranalyse unterstützen: Listen von FP/FN pro Tool exportieren (manuelle Kategorisierung)

### 7) Export & Reporting
- Ground Truth (inkl. Kriterien & Kommentare) als CSV/JSON
- Tool-Outputs je Projekt als CSV/JSON
- Metrik-Tabellen + Overlap als CSV + Markdown/HTML-Report
- Logs der Runs (Build/Analyse/Reliability)

---

## Architekturvorschlag (präferiert)
**Client-Server Web-App**:
- Backend: Java 17 + Spring Boot (REST)
- Frontend: React oder Angular (minimal, Form-lastig)

**Minimal-Fallback (falls UI zu teuer):**
- Java CLI + CSV-Export/Import für Labeling (Excel), dann Auswertung im Tool

---

## Datenmodelle (DTOs, pragmatisch)
- ProjectDTO: { id, name, path, buildStatus, analysisStatus }
- CandidateDTO: { id, projectId, className, fromSonar, fromJDeo, fromBaseline, isExtra, labelStatus }
- LabelDTO: { candidateId, k1, k2, k3, k4, comment, result }
- ReliabilityDTO: { agreementPercent, cohenKappa, ac1, conflicts:[...] }
- MetricsDTO: { toolName, precision, recall, f1, specificity, mcc, breakdownByProject:{...} }
- OverlapDTO: { pair:(toolA,toolB), jaccard, both, onlyA, onlyB }

---

## Code- und Repo-Standards

### Java
- Google Java Style (Formatierung konsistent, keine “clevere” Sonderformate)
- Klare Layering-Struktur (package by feature oder classic layered; aber konsistent)
- Keine Magie: Schwellenwerte/Seeds/Tool-Versionen zentral konfigurieren (aber Baseline-Schwellen “fix” lassen)

### Logging
- Jede Tool-Phase loggt: Start/Ende, Dauer, #Candidates, Fehlerdetails
- Fehler in einem Tool dürfen den Run nicht komplett abbrechen (weiter mit anderen Tools)

### Testing (Minimum)
- Unit-Tests: Metrik-Berechnung, Confusion-Matrix/Metriken, Kandidaten-Union/Sampling (Seed!)
- Integration-Tests: “Mini-Projekt” mit wenigen Klassen (inkl. absichtlicher God-Class-Klasse)
- E2E optional (nice-to-have), Fokus auf Korrektheit + Reproduzierbarkeit

### Definition of Done (pro Feature)
- Funktion erfüllt FR + negative Fälle (Fehlerpfade) behandelt
- Tests mindestens für Kernlogik vorhanden
- Logging sinnvoll, Exportformate stabil
- Dokumentation/README aktualisiert

---

## Codex-CLI Arbeitsweise (Workflows & Prompt-Playbook)

### Leitplanken (damit das Repo nicht explodiert)
- **Plan vor Code:** Erst Plan (Dateien/Interfaces/Tests/Risiken), dann Implementierung in kleinen Schritten.
- **Kleine Blast-Radius:** Pro Runde max. ~5–8 Dateien ändern. Keine “nebenbei”-Refactors.
- **Stop-the-line:** Wenn Tests/Build rot → erst fixen, nicht “weiter implementieren”.
- **Determinismus überall:** Sampling immer mit Seed, Exporte sortiert, Tool-Versionen in Outputs.
- **Keine Schwellenwert-Spielerei:** Baseline-Regel ist fix, Sonar/JDeodorant out-of-the-box.

### Arbeitsaufteilung: Slices statt Mega-Features
Arbeite in **end-to-end lauffähigen Scheiben**, die jeweils messbar fertig sind:

1) **DTOs + Export/Import** (CSV/JSON) für Kandidaten + Labels (noch ohne Tools)  
2) **Baseline Analyzer** (JavaParser) → Kandidatenliste → Export  
3) **Union + Sampling (Seed!)** → Label-Input CSV (inkl. Extra-Negativ-Samples)  
4) **Metrics Engine** (Confusion Matrix, Precision/Recall/F1, optional MCC/Specificity) + Report-Export  
5) **SonarQube Integration** (Docker/Scanner/API) → Kandidatenliste  
6) **JDeodorant Integration** (headless bevorzugt; sonst klarer CSV Import-Fallback)  
7) **Reliability** (Agreement, Cohen’s Kappa, Gwet’s AC1) + Konfliktliste + Edit-Loop

### Prompt-Template (Copy/Paste)
**Immer so prompten**, damit der Agent nicht rät:

- **Ziel:** Was soll am Ende konkret entstehen (Output/Endpoint/Datei)?
- **Kontext:** Wo im Repo? Welche Module/Packages/DTOs existieren?
- **Constraints:** Java 17, Versionen, keine Breaking Changes, Seed fix, keine Threshold-Tuning.
- **Akzeptanzkriterien:** Welche Tests/Checks müssen grün sein? Welche Datei muss rausfallen?

**Beispiel:**
> Ziel: Implementiere `BaselineAnalyzer` (JavaParser) der pro Klasse WMC/TCC/ATFD/CBO/LOC berechnet und God-Class nach fixen Schwellen markiert.  
> Kontext: Neues Package `...baseline` im Backend, DTOs existieren.  
> Constraints: Java 17, JavaParser 3.27.1, deterministische Sortierung, kein Threshold-Tuning.  
> Akzeptanz: Unit-Tests (Edge Cases) + goldene CSV-Ausgabe für Mini-Testprojekt, `mvn test` grün.

### Zwei-Phasen-Modus (empfohlen)
1) **Plan-only Prompt**
   - “Erstelle Implementierungsplan + betroffene Dateien + Tests + Risiken. Kein Code.”
2) **Implement Prompt**
   - “Implementiere nur Schritt 1–2 aus dem Plan. Keine weiteren Änderungen.”

### Änderungsregeln (streng, aber rettet Zeit)
- **Max Dateien:** “Ändere maximal X Dateien.”
- **No drive-by refactor:** “Kein Umbenennen/Umstrukturieren ohne Not.”
- **Neue Dependencies:** nur wenn nötig + Alternativen nennen + begründen.
- **Diff-Übersicht zuerst:** Bei größeren Änderungen erst “geplante Datei-zu-Datei Änderungen” ausgeben lassen.

### Testing: Minimum, das du wirklich brauchst
- Kernlogik (Metriken/Sampling/Confusion Matrix) **immer Unit-Tests**.
- Integration (Sonar/JDeo) mindestens **Smoke Tests** + Fixture-Dateien.
- **Goldene Dateien** (expected.csv/json) im Test: actual erzeugen → diff.

**Edge Cases (müssen abgedeckt sein):**
- leere Inputs
- Division-by-zero (Precision/Recall)
- 100% Agreement → Kappa darf nicht crashen (Varianz=0)
- Unresolved Class Names (Sonar liefert Filepaths etc.)

### “Golden Inputs” (Mini-Projekt) — Pflicht
Lege ein kleines Testprojekt (5–10 Klassen) an mit:
- 1 absichtlicher God Class
- 1 große aber kohäsive Klasse (FP-Falle)
- 1 kleine aber stark gekoppelte Klasse
Damit erkennst du sofort, wenn du “aus Versehen” das Kriterium verschiebst.

### Integration-Prompts (konkret)

**SonarQube (in 4 Schritten)**
1) Docker Compose + Healthcheck
2) Scanner-Run (Maven/Gradle) mit reproduzierbarer Config
3) Issues pullen für Rule **java:S6539** (“Monster Class”)
4) Mapping → `CandidateDTO` (FQCN), unresolved sauber markieren

Prompt:
> Implementiere `SonarResultFetcher`: ruft Issues für Rule `java:S6539` je Projekt ab und mappt auf fully qualified class names. Unit-Test mit mocked API-JSON. Bei nicht auflösbaren Klassennamen: `unresolved=true` + Logeintrag.

**JDeodorant (realistisch bleiben)**
- Headless/Automatisierung bevorzugt; falls nicht stabil: **CSV Import-Fallback** (manueller Export aus IDE → Tool importiert).

Prompt:
> Implementiere `JDeodorantImporter`, der eine CSV mit Klassennamen einliest und Kandidaten markiert. Validierung + klare Fehlermeldungen. Logge, dass der “manual import path” aktiv ist.

### Prompt-Bibliothek (kurz, brutal nützlich)

**A) Nur Analyse, kein Code**
> Analysiere die bestehende Struktur und schlage minimale Änderungen vor. Kein Code.

**B) Implementiere nur ein Package**
> Implementiere ausschließlich `.../baseline/*`. Änderungen außerhalb nur, wenn zwingend (DTO-Imports). Keine Umstrukturierung.

**C) Fix nur einen Bug**
> Fix ausschließlich den Fehler in `<Datei/Klasse>`. Keine Formatierung, kein Refactor, keine neuen Features.

**D) Tests nachziehen**
> Ergänze Unit-Tests für Edge Cases: leere Inputs, Division-by-zero, 100% Agreement (Kappa Varianz=0), deterministisches Sampling (Seed).

### Qualitäts-Tripwire (wenn das passiert: abbrechen & korrigieren)
- nicht-deterministische Outputs (unsortiert, Random ohne Seed)
- “silent failures” ohne Status/Logs
- Threshold-Tuning “damit’s besser aussieht”
- Analyse-Scope enthält Tests/Generated Code
- JDeodorant/Sonar Ergebnisse werden “schön gerechnet” statt sauber gemappt


### Grundregel
Bevor du Code erzeugst: **Plan → betroffene Dateien → Risiken → Tests/Checks**.

### Workflow: Feature-Implementierung
1) Kurze technische Skizze (Endpoints, Datenfluss, DTOs)
2) Implementiere minimal (happy path)
3) Ergänze Fehlerpfade & Logging
4) Schreibe Tests
5) Aktualisiere Doku/README + Beispiel-Outputs

### Workflow: Refactor
- Nur refactoren, wenn Tests grün oder vorher minimal Tests ergänzen
- Keine behavior changes ohne expliziten Auftrag
- Kleine Schritte, klar benannte Commits

### Workflow: SonarQube/JDeodorant Integration
- Konfiguration version-pin + reproduzierbar (Docker compose / Skripte)
- Ergebnis-Extraktion robust gegen API-Änderungen (Parsing/Mapping kapseln)
- Bei Tool-Fail: klare Fehlermeldung + Weiterlauf

### Workflow: Report/Export
- Exporte deterministisch (sortierte Ausgaben)
- Schema-Version in Exportkopf/Metadaten mitschreiben (z.B. reportVersion, toolVersions, commitHash)

---

## “Red Flags” (bitte vermeiden)
- Threshold-Tuning “damit es besser aussieht”
- Nicht deterministisches Sampling ohne Seed
- “Silent failures” ohne Log/Status
- UI-Features, die mehr Zeit fressen als der Erkenntnisgewinn rechtfertigt
- Vermischung von Testcode/Generated Code in Analyse-Scope

---

## Quick Commands (für den Agenten)
- Backend starten: `mvn spring-boot:run`
- Tests: `mvn test`
- Format/Check (wenn vorhanden): `mvn spotless:apply` / `mvn checkstyle:check`
- Sonar via Docker: `docker compose up -d` (wenn compose vorhanden)

(Die konkreten Skripte/Compose-Files werden im Repo definiert.)
