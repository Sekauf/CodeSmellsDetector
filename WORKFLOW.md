# WORKFLOW.md — Schritt-für-Schritt pro User Story

## Vorbereitung (einmalig)

```bash
cd ~/IdeaProjects/CodeSmellsDetector   # oder dein Projekt-Pfad
mvn test                                # Sicherstellen: alles grün
git add -A && git commit -m "Stand vor Sprint"
```

---

## Pro Story: 6 Schritte

### Schritt 1 — Branch erstellen

```bash
git checkout main
git checkout -b US-01-label-dto
```

### Schritt 2 — CLI starten + Prompt pasten

**Claude Code:**
```bash
claude
```

**Codex CLI:**
```bash
codex
```

Prompt aus `PROMPTS.md` kopieren und einfügen. Beide Tools werden automatisch:
1. Den Plan-First-Skill triggern (steht in CLAUDE.md / AGENTS.md)
2. Dir einen Plan zeigen (Dateien, Methoden, Tests, Risiken)
3. Fragen: "Plan steht. Soll ich implementieren?"

**Du prüfst den Plan:**
- Sind nur erlaubte Dateien dabei? (keine aus baseline/sonar/jdeodorant)
- Nutzt er bestehende Utilities? (CandidateCsvUtil etc.)
- Sind Tests dabei?

**Wenn Plan okay:** "Ja, implementiere."  
**Wenn nicht:** "Nein, ändere X" oder beschreibe was anders sein soll.

> **Falls der Agent den Plan überspringt:**
> - Claude Code: `Stopp. Zeig mir erst den Plan gemäß /plan-first Skill.`
> - Codex CLI: `Stopp. Nutze den $plan-first Skill bevor du Code schreibst.`

### Schritt 3 — Prüfen was der Agent gemacht hat

```bash
# Welche Dateien wurden angefasst?
git diff --stat

# Checklist:
# □ Nur erwartete Dateien? (neue in labeling/evaluation/reporting + Tests)
# □ Keine Änderungen in baseline/, sonar/, jdeodorant/, metrics/, sampling/?
# □ Keine neuen Dependencies in pom.xml?
```

**Wenn unerwartete Dateien geändert wurden:**
```bash
# Einzelne Datei zurücksetzen
git checkout -- src/main/java/org/example/baseline/CandidateDTO.java

# Oder alles zurücksetzen und nochmal prompten
git checkout .
```

### Schritt 4 — Tests laufen lassen

```bash
mvn test
```

**Grün?** → Weiter zu Schritt 5.

**Rot?** → Dem Agent den Fehler geben:
```
mvn test ist rot. Hier ist der Fehler:
[Fehler-Output einfügen]

Fix nur diesen Fehler. Keine anderen Änderungen.
```

### Schritt 5 — Schneller Code-Check (30 Sekunden)

Öffne die neuen Dateien in IntelliJ und prüfe:

- □ Klassen nicht zu lang? (≤ 200 Zeilen)
- □ Methoden nicht zu lang? (≤ 30 Zeilen)
- □ Javadoc auf public Methoden?
- □ Bestehende Utilities genutzt? (CandidateCsvUtil, ResultExporter etc.)
- □ Keine Copy-Paste-Duplikate?
- □ Logging vorhanden? (Start/Ende/Counts)

**Wenn was nicht passt:**
```
Die Klasse XY ist zu lang (280 Zeilen). Extrahiere private Hilfsmethoden.
Keine anderen Änderungen.
```

### Schritt 6 — Commit & Merge

```bash
git add -A
git commit -m "US-01: LabelDTO + LabelCsvExporter"
git checkout main
git merge US-01-label-dto
```

**Optional:** Branch löschen
```bash
git branch -d US-01-label-dto
```

---

## Dann: AGENTS.md updaten

Öffne `AGENTS.md` und ändere den Story-Status:

```
| US-01 | ✅ | LabelDTO + Labeling-CSV-Export |
```

**Das ist wichtig** — der Agent liest beim nächsten Prompt den Tracker und weiß was existiert.

---

## Ablauf-Beispiel: US-01

```bash
# 1. Branch
git checkout -b US-01-label-dto

# 2. Claude Code starten
claude

# 3. Prompt einfügen (aus PROMPTS.md, US-01 Block)
#    Claude zeigt automatisch den Plan:
#
#    📋 Plan:
#    NEU: src/main/java/org/example/labeling/LabelDTO.java
#    NEU: src/main/java/org/example/labeling/LabelCsvExporter.java
#    NEU: src/test/java/org/example/labeling/LabelDTOTest.java
#    
#    Methoden: LabelDTO.deriveLabel(), LabelCsvExporter.export()
#    Tests: deriveLabel-Kombinationen, CSV-Export mit 3 Kandidaten
#    Risiken: keine, neue Dateien only
#    
#    "Plan steht. Soll ich implementieren?"
#
#    → "Ja, implementiere."

# 4. Nach Implementierung: prüfen
# (In einem neuen Terminal oder /exit aus Claude)
git diff --stat
#  src/main/java/org/example/labeling/LabelDTO.java        | 45 ++++
#  src/main/java/org/example/labeling/LabelCsvExporter.java | 78 ++++
#  src/test/java/org/example/labeling/LabelDTOTest.java     | 62 ++++
#  3 files changed, 185 insertions(+)
# ✓ Nur 3 neue Dateien im richtigen Package

# 5. Tests
mvn test
# ✓ 85 Tests, 0 Failures

# 6. Commit
git add -A && git commit -m "US-01: LabelDTO + LabelCsvExporter"
git checkout main && git merge US-01-label-dto

# 7. AGENTS.md updaten: US-01 → ✅
```

---

## Wenn was schiefgeht

| Problem | Lösung |
|---------|--------|
| Agent ändert zu viele Dateien | `git checkout .` → nochmal prompten mit "Max 3 Dateien" |
| Agent baut Utils die es schon gibt | Prompt ergänzen: "Nutze CandidateCsvUtil.escapeCsvField(), bau keinen eigenen CSV-Writer" |
| Tests rot nach Agent-Änderung | Fehler-Output an Agent geben: "Fix nur diesen Fehler" |
| Agent will CandidateDTO ändern | `git checkout -- src/.../CandidateDTO.java` + Hinweis: "CandidateDTO nicht ändern, erstelle stattdessen ein neues DTO" |
| Code zu lang / unstrukturiert | Nachprompt: "Extrahiere Methode X in private Hilfsmethode. Keine anderen Änderungen." |
| Merge-Konflikte | `git merge --abort` → Branch rebasing: `git rebase main` auf dem Feature-Branch |
| Story zu groß für einen Prompt | In 2 Prompts teilen: erst Datenmodell+Export, dann Import+Tests |

---

## Reihenfolge der Stories

```
Tag 1:  US-01 → US-02
Tag 2:  US-03 → US-04
Tag 3:  US-05
Tag 4:  US-06 → US-07
Tag 5:  US-08 → US-09
Tag 6:  US-10 → US-11
Tag 7:  US-12
Tag 8:  US-13
Tag 9:  US-14
```

Abhängigkeiten: US-01 → US-02 → US-03/04 → US-07 → US-09/10/11 → US-12 → US-13.
US-05 braucht echte Labels (erst nach manuellem Labeling). US-06 und US-08 sind unabhängig.
