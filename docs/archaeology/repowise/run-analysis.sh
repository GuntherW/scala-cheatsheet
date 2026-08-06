#!/usr/bin/env bash
# repowise Starter für esap-hub-service
#
# Aufruf:
#   ./run-analysis.sh install    # repowise installieren
#   ./run-analysis.sh init       # Index erstellen (einmalig, ~30s)
#   ./run-analysis.sh update     # Index aktualisieren (nach neuen Commits)
#   ./run-analysis.sh serve      # Dashboard auf http://localhost:3000 starten
#   ./run-analysis.sh health     # Code-Health-Report auf der Konsole
#   ./run-analysis.sh targets    # Refactoring-Kandidaten anzeigen
#   ./run-analysis.sh deadcode   # Toten Code anzeigen

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"

check() {
  command -v repowise &>/dev/null && return 0
  echo "ERROR: repowise nicht installiert. Ausführen: ./run-analysis.sh install"
  exit 1
}

case "${1:-serve}" in
  install)
    pip install repowise --break-system-packages
    repowise --version
    ;;
  init)
    check
    cd "${REPO_ROOT}"
    echo "Index wird erstellt (~30 Sekunden) ..."
    repowise init --no-prose -y
    ;;
  update)
    check
    cd "${REPO_ROOT}"
    repowise update
    ;;
  serve)
    check
    cd "${REPO_ROOT}"
    echo "Dashboard: http://localhost:3000"
    echo "Beenden mit: Ctrl+C"
    echo ""

    # API-Server und Frontend separat starten.
    # Hintergrund: 'repowise serve' beendet uvicorn nach wenigen Sekunden
    # wenn kein interaktives Terminal vorhanden ist (kein stdin).
    # Workaround: beide Prozesse explizit mit stdin=/dev/null starten.

    python3 -m uvicorn \
      "repowise.server.app:create_app" \
      --factory \
      --host 127.0.0.1 \
      --port 7337 \
      --log-level warning \
      < /dev/null >> /tmp/repowise-api.log 2>&1 &
    API_PID=$!

    sleep 3

    REPOWISE_API_URL=http://localhost:7337 PORT=3000 HOSTNAME=0.0.0.0 \
      node ~/.repowise/web/server.js \
      < /dev/null >> /tmp/repowise-ui.log 2>&1 &
    UI_PID=$!

    echo "API PID: ${API_PID}  |  UI PID: ${UI_PID}"
    echo "Logs: /tmp/repowise-api.log  /tmp/repowise-ui.log"
    echo ""

    trap "kill ${API_PID} ${UI_PID} 2>/dev/null" EXIT INT TERM
    wait
    ;;
  health)
    check
    cd "${REPO_ROOT}"
    repowise health
    ;;
  targets)
    check
    cd "${REPO_ROOT}"
    repowise health --refactoring-targets
    ;;
  deadcode)
    check
    cd "${REPO_ROOT}"
    repowise dead-code
    ;;
  *)
    echo "Verwendung: $0 [install|init|update|serve|health|targets|deadcode]"
    exit 1
    ;;
esac
