#!/usr/bin/env bash
# SonarQube Analyse-Pipeline für esap-hub-service
#
# Voraussetzung: SonarQube läuft unter http://localhost:9000
#   docker run -d --name sonarqube -p 9000:9000 sonarqube:community
#
# Aufruf:
#   export SONAR_TOKEN="sqp_..."   # Token aus SonarQube (Schritt 4 der README)
#   ./run-analysis.sh
#
# Optionen:
#   ./run-analysis.sh build        # Nur Bytecode bauen
#   ./run-analysis.sh scan         # Nur Scanner ausführen (Token muss gesetzt sein)
#   ./run-analysis.sh setup        # Projekt + Token automatisch anlegen (nur beim ersten Mal)
#   ./run-analysis.sh open         # Dashboard im Browser öffnen

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
SONAR_URL="http://localhost:9000"
PROJECT_KEY="esap-hub-service"

# SonarScanner: entweder im PATH oder unter ~/tools/
SCANNER=$(command -v sonar-scanner 2>/dev/null || \
  ls ~/tools/sonar-scanner-*/bin/sonar-scanner 2>/dev/null | tail -1 || \
  echo "")

check_scanner() {
  if [[ -z "${SCANNER}" ]]; then
    echo "ERROR: sonar-scanner nicht gefunden."
    echo ""
    echo "Herunterladen:"
    echo "  curl -L https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-7.1.0.4889-linux-x64.zip -o /tmp/sonar-scanner.zip"
    echo "  unzip /tmp/sonar-scanner.zip -d ~/tools/"
    exit 1
  fi
}

check_token() {
  if [[ -z "${SONAR_TOKEN:-}" ]]; then
    echo "ERROR: SONAR_TOKEN nicht gesetzt."
    echo ""
    echo "Token erzeugen (einmalig):"
    echo "  ./run-analysis.sh setup"
    echo ""
    echo "Dann exportieren:"
    echo "  export SONAR_TOKEN=\"sqp_...\""
    exit 1
  fi
}

check_server() {
  local status
  status=$(curl -s "${SONAR_URL}/api/system/status" 2>/dev/null \
    | python3 -c "import sys,json; print(json.load(sys.stdin).get('status','DOWN'))" 2>/dev/null || echo "DOWN")
  if [[ "${status}" != "UP" ]]; then
    echo "ERROR: SonarQube ist nicht erreichbar (Status: ${status})"
    echo ""
    echo "Server starten:"
    echo "  docker start sonarqube"
    echo "  # oder neu:"
    echo "  docker run -d --name sonarqube -p 9000:9000 sonarqube:community"
    exit 1
  fi
}

do_setup() {
  echo "=== Setup: Projekt + Token anlegen ==="
  echo ""
  read -rp "SonarQube Admin-Passwort [Sonar-Archaeology1]: " ADMIN_PASS
  ADMIN_PASS="${ADMIN_PASS:-Sonar-Archaeology1}"

  # Projekt anlegen (Fehler ignorieren falls schon vorhanden)
  curl -s -u "admin:${ADMIN_PASS}" \
    -X POST "${SONAR_URL}/api/projects/create" \
    -d "name=${PROJECT_KEY}&project=${PROJECT_KEY}&visibility=private" \
    > /dev/null

  echo "Projekt '${PROJECT_KEY}' angelegt (oder bereits vorhanden)"

  # Token erzeugen
  TOKEN_NAME="esap-scan-$(date +%s)"
  TOKEN_JSON=$(curl -s -u "admin:${ADMIN_PASS}" \
    -X POST "${SONAR_URL}/api/user_tokens/generate" \
    -d "name=${TOKEN_NAME}&type=PROJECT_ANALYSIS_TOKEN&projectKey=${PROJECT_KEY}")
  TOKEN=$(echo "${TOKEN_JSON}" | python3 -c "import sys,json; print(json.load(sys.stdin).get('token','ERROR'))")

  if [[ "${TOKEN}" == "ERROR" || -z "${TOKEN}" ]]; then
    echo "ERROR: Token konnte nicht erzeugt werden. Antwort: ${TOKEN_JSON}"
    exit 1
  fi

  echo ""
  echo "Token erzeugt. Jetzt ausführen:"
  echo ""
  echo "  export SONAR_TOKEN=\"${TOKEN}\""
  echo "  ./run-analysis.sh"
  echo ""
  echo "Oder dauerhaft in ~/.bashrc speichern:"
  echo "  echo 'export SONAR_TOKEN_ESAP=\"${TOKEN}\"' >> ~/.bashrc"
}

do_build() {
  echo "=== Bytecode bauen ==="
  cd "${REPO_ROOT}"
  ./gradlew :service:classes :contract:classes 2>&1 | tail -5
  echo "Build abgeschlossen."
}

do_scan() {
  check_scanner
  check_token
  check_server

  echo "=== SonarQube Analyse ==="
  echo "Projekt:  ${PROJECT_KEY}"
  echo "Server:   ${SONAR_URL}"
  echo "Scanner:  ${SCANNER}"
  echo ""

  cd "${REPO_ROOT}"

  # JaCoCo-Report optional einbinden
  JACOCO_OPT=""
  JACOCO_XML="service/build/reports/jacoco/test/jacocoTestReport.xml"
  if [[ -f "${JACOCO_XML}" ]]; then
    JACOCO_OPT="-Dsonar.coverage.jacoco.xmlReportPaths=${JACOCO_XML}"
    echo "JaCoCo-Report gefunden: ${JACOCO_XML}"
  else
    echo "JaCoCo-Report nicht gefunden – Coverage wird nicht angezeigt."
    echo "  ./gradlew :service:test :service:jacocoTestReport"
  fi

  # Bytecode optional einbinden
  BINARIES_OPT=""
  if [[ -d "service/build/classes/kotlin/main" ]]; then
    BINARIES_OPT="-Dsonar.java.binaries=service/build/classes/kotlin/main,contract/build/classes/java/main"
    echo "Bytecode gefunden – Tiefenanalyse aktiv."
  else
    echo "Kein Bytecode – nur Quelltextanalyse."
    echo "  ./gradlew :service:classes :contract:classes"
  fi

  echo ""

  "${SCANNER}" \
    -Dsonar.projectKey="${PROJECT_KEY}" \
    -Dsonar.host.url="${SONAR_URL}" \
    -Dsonar.token="${SONAR_TOKEN}" \
    -Dsonar.sources=service/src/main/kotlin,contract/src/main/java,esap-types/src/main/kotlin \
    -Dsonar.tests=service/src/test/kotlin \
    ${BINARIES_OPT} \
    ${JACOCO_OPT} \
    2>&1 | grep -v "^$" | grep -E "Sensor|WARN|ERROR|SUCCESS|FAILURE|results at"

  echo ""
  echo "Dashboard: ${SONAR_URL}/dashboard?id=${PROJECT_KEY}"
}

do_open() {
  local url="${SONAR_URL}/dashboard?id=${PROJECT_KEY}"
  xdg-open "${url}" 2>/dev/null || open "${url}" 2>/dev/null || echo "Browser öffnen: ${url}"
}

CMD="${1:-scan}"
case "${CMD}" in
  setup)    check_server; do_setup ;;
  build)    do_build ;;
  scan)     do_scan ;;
  open)     do_open ;;
  all)      do_build; do_scan ;;
  *)
    echo "Verwendung: $0 [setup|build|scan|open|all]"
    echo ""
    echo "  setup  – Projekt und Token in SonarQube anlegen (einmalig)"
    echo "  build  – Bytecode bauen (./gradlew classes)"
    echo "  scan   – Analyse ausführen (SONAR_TOKEN muss gesetzt sein)"
    echo "  open   – Dashboard im Browser öffnen"
    echo "  all    – build + scan"
    exit 1
    ;;
esac
