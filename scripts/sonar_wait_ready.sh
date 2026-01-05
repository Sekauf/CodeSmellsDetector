#!/usr/bin/env bash
set -euo pipefail

# Waits for SonarQube to report UP via /api/system/status.
# Usage:
#   ./scripts/sonar_wait_ready.sh [host] [timeout_seconds]
# Example:
#   ./scripts/sonar_wait_ready.sh http://localhost:9000 120

HOST_URL="${1:-http://localhost:9000}"
TIMEOUT_SECONDS="${2:-120}"

START_TS="$(date +%s)"
STATUS_URL="${HOST_URL%/}/api/system/status"

request_status() {
  if command -v curl >/dev/null 2>&1; then
    curl -fsS "${STATUS_URL}" || true
  elif command -v wget >/dev/null 2>&1; then
    wget -qO- "${STATUS_URL}" || true
  else
    echo "Neither curl nor wget found in PATH." >&2
    return 2
  fi
}

while true; do
  BODY="$(request_status || true)"
  if echo "${BODY}" | grep -qiE '"status"\s*:\s*"(UP|GREEN)"'; then
    echo "SonarQube is ready."
    exit 0
  fi

  NOW_TS="$(date +%s)"
  ELAPSED="$((NOW_TS - START_TS))"
  if [ "${ELAPSED}" -ge "${TIMEOUT_SECONDS}" ]; then
    echo "Timed out waiting for SonarQube after ${TIMEOUT_SECONDS}s." >&2
    exit 1
  fi
  sleep 2
done
