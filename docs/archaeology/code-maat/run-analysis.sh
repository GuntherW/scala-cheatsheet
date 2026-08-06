#!/usr/bin/env bash
# code-maat Analyse-Pipeline für esap-hub-service
#
# Voraussetzungen:
#   - Java im PATH
#   - Python 3 im PATH
#
# Aufruf:
#   ./run-analysis.sh           # Vollständige Analyse
#   ./run-analysis.sh log       # Nur Git-Log erzeugen
#   ./run-analysis.sh analyse   # Nur Analysen ausführen (Log muss vorhanden sein)
#   ./run-analysis.sh visualize # Nur HTML-Visualisierung neu generieren
#   ./run-analysis.sh open      # Visualisierung im Browser öffnen

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
JAR="${SCRIPT_DIR}/tools/code-maat.jar"
LOG="${SCRIPT_DIR}/data/git.log"
DATA="${SCRIPT_DIR}/data"
RESULTS="${SCRIPT_DIR}/results"
VISUALIZE="${SCRIPT_DIR}/visualize"

if [[ ! -f "${JAR}" ]]; then
    echo "ERROR: code-maat.jar nicht gefunden unter: ${JAR}"
    echo "Bitte README befolgen (Download-Schritt)."
    exit 1
fi

mkdir -p "${DATA}" "${RESULTS}" "${VISUALIZE}"

do_log() {
    echo "=== [1] Git-Log erzeugen ==="
    git -C "${REPO_ROOT}" log --all --numstat --date=short \
        --pretty=format:'--%H--%ad--%aN' \
        -- "service/src" "contract/src" "esap-types/src" \
        > "${LOG}"
    echo "Commits im Log: $(grep -c '^--[a-f0-9]\{40\}' "${LOG}")"
}

do_analyse() {
    echo "=== [2] Analysen ausführen ==="
    if [[ ! -f "${LOG}" ]]; then
        echo "ERROR: Kein Git-Log gefunden. Zuerst './run-analysis.sh log' ausführen."
        exit 1
    fi

    for analysis in revisions coupling authors entity-churn entity-effort age soc summary; do
        output=$(java -jar "${JAR}" -l "${LOG}" -c git2 -a "${analysis}" 2>/dev/null || true)
        if [[ -n "${output}" ]]; then
            echo "${output}" > "${RESULTS}/${analysis}.csv"
            lines=$(echo "${output}" | wc -l)
            printf "  %-18s %4d Zeilen\n" "${analysis}:" "${lines}"
        else
            echo "  WARN: ${analysis} lieferte keine Ergebnisse"
        fi
    done

    # Hotspot-Merge: Revisions × LOC
    echo ""
    echo "=== [3] Hotspot-Merge (Revisions × LOC) ==="
    python3 "${SCRIPT_DIR}/tools/merge_hotspots.py" \
        "${RESULTS}/revisions.csv" \
        "${DATA}/lines.csv" \
        > "${RESULTS}/hotspots.csv"
    echo "  hotspots.csv: $(wc -l < "${RESULTS}/hotspots.csv") Einträge"
}

do_loc() {
    echo "=== LOC zählen ==="
    echo "entity,lines" > "${DATA}/lines.csv"
    find "${REPO_ROOT}/service/src/main/kotlin" \
         "${REPO_ROOT}/contract/src/main/java" \
         -name "*.kt" -o -name "*.java" 2>/dev/null | \
    xargs wc -l 2>/dev/null | \
    grep -v "^[[:space:]]*0\|total$" | \
    awk '{gsub(/^[[:space:]]+/,""); gsub(/^.*esap-hub-service\//,"",$2); print $2","$1}' \
    >> "${DATA}/lines.csv"
    echo "  $(wc -l < "${DATA}/lines.csv") Dateien erfasst"
}

do_visualize() {
    echo "=== [4] HTML-Visualisierung generieren ==="
    python3 "${SCRIPT_DIR}/tools/generate_html.py" \
        "${RESULTS}/hotspots.csv" \
        "${RESULTS}/coupling.csv" \
        "${RESULTS}/authors.csv" \
        "${VISUALIZE}/hotspots.html"
    echo "  Geöffnet werden kann: ${VISUALIZE}/hotspots.html"
}

do_open() {
    local file="${VISUALIZE}/hotspots.html"
    if [[ ! -f "${file}" ]]; then
        echo "Visualisierung noch nicht generiert. Starte vollständige Analyse..."
        do_log; do_loc; do_analyse; do_visualize
    fi
    xdg-open "${file}" 2>/dev/null || open "${file}" 2>/dev/null || \
        echo "Bitte manuell öffnen: file://${file}"
}

CMD="${1:-all}"
case "${CMD}" in
    log)        do_log ;;
    loc)        do_loc ;;
    analyse)    do_analyse ;;
    visualize)  do_visualize ;;
    open)       do_open ;;
    all)
        do_log
        do_loc
        do_analyse
        do_visualize
        echo ""
        echo "=== Fertig! ==="
        echo "Visualisierung: file://${VISUALIZE}/hotspots.html"
        echo "CSV-Ergebnisse: ${RESULTS}/"
        ;;
    *)
        echo "Unbekannter Befehl: ${CMD}"
        echo "Optionen: log | loc | analyse | visualize | open | all"
        exit 1
        ;;
esac
