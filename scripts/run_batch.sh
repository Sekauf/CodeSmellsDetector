#!/usr/bin/env bash
# run_batch.sh — US-16: Multi-Projekt-Batch-Modus
#
# Fuehrt die God-Class-Analyse sequenziell fuer mehrere Projekte durch.
# Jedes Projekt erhaelt ein eigenes Ausgabeverzeichnis unter OUTPUT_BASE.
#
# Usage: ./scripts/run_batch.sh
#
# Konfiguration:
#   Passe die drei Arrays PROJECTS, PATHS und JD_CSVS unten an.
#   Alle drei Arrays muessen gleich lang sein.
#   Ein leerer JD_CSVS-Eintrag ("") ueberspringt den JDeodorant-Import.
#
# Optionale Env-Variablen:
#   JAR          — Pfad zum JAR         (default: target/CodeSmellsDetector.jar)
#   OUTPUT_BASE  — Basisverzeichnis     (default: output/batch)
#   SONAR        — true/false           (default: false)

set -e

# ── Konfiguration ────────────────────────────────────────────────────────────
JAR="${JAR:-target/CodeSmellsDetector.jar}"
OUTPUT_BASE="${OUTPUT_BASE:-output/batch}"
SONAR="${SONAR:-false}"

# Projekt-Arrays — alle drei muessen gleich lang sein
PROJECTS=("projectA"    "projectB"    "projectC")
PATHS=(   "/path/to/A"  "/path/to/B"  "/path/to/C")
JD_CSVS=( "jd-A.csv"    "jd-B.csv"    "")

# ── Hilfsfunktionen ──────────────────────────────────────────────────────────
log() { echo "[batch] $(date +%H:%M:%S) $*"; }

die() {
    echo "[batch] FEHLER: $*" >&2
    exit 1
}

# ── Validierung ──────────────────────────────────────────────────────────────
if [ ! -f "${JAR}" ]; then
    die "JAR nicht gefunden: ${JAR} — bitte zuerst 'mvn package' ausfuehren."
fi

if [ "${#PROJECTS[@]}" -ne "${#PATHS[@]}" ] || [ "${#PROJECTS[@]}" -ne "${#JD_CSVS[@]}" ]; then
    die "PROJECTS, PATHS und JD_CSVS muessen gleich lang sein."
fi

if [ "${#PROJECTS[@]}" -eq 0 ]; then
    die "Keine Projekte konfiguriert."
fi

# ── Einzelprojekt-Analyse ────────────────────────────────────────────────────
run_project() {
    local name="$1"
    local path="$2"
    local jd_csv="$3"
    local out="${OUTPUT_BASE}/${name}"

    log "=== ${name} ==="
    log "  Projekt-Pfad:    ${path}"
    log "  Ausgabe:         ${out}"
    log "  JDeodorant-CSV:  ${jd_csv:-<kein Import>}"
    log "  SonarQube:       ${SONAR}"

    mkdir -p "${out}"

    # Kommando zusammenbauen
    local cmd=(java -jar "${JAR}"
        --project "${path}"
        --output  "${out}")

    # Optional: SonarQube
    if [ "${SONAR}" = "true" ]; then
        cmd+=(--run-sonar)
    fi

    # Optional: JDeodorant-CSV (leerer String = kein Import)
    if [ -n "${jd_csv}" ]; then
        cmd+=(--jdeodorant-csv "${jd_csv}")
    fi

    "${cmd[@]}"

    log "Fertig: ${name}"
}

# ── Batch-Lauf ───────────────────────────────────────────────────────────────
log "Starte Batch-Analyse fuer ${#PROJECTS[@]} Projekte..."
mkdir -p "${OUTPUT_BASE}"

for i in "${!PROJECTS[@]}"; do
    run_project "${PROJECTS[$i]}" "${PATHS[$i]}" "${JD_CSVS[$i]}"
done

log "Alle ${#PROJECTS[@]} Projekte abgeschlossen. Ergebnisse in: ${OUTPUT_BASE}/"
