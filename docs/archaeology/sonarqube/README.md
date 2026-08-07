# SonarQube Community Edition – Einrichtung und Analyse

Diese Anleitung beschreibt, wie man ein **beliebiges Java/Kotlin-Projekt** mit
SonarQube analysiert. Sie ist bewusst allgemein gehalten und nicht auf ein
bestimmtes Projekt zugeschnitten.

---

## Was SonarQube analysiert

| Kategorie | Beispiele |
|---|---|
| **Bugs** | Null-Pointer-Risiken, fehlerhafte Bedingungen, Resource-Leaks |
| **Vulnerabilities** | SQL-Injection, unsichere Konfiguration (OWASP/CWE-klassifiziert) |
| **Security Hotspots** | Stellen, die manuell geprüft werden sollten |
| **Code Smells** | Zu lange Methoden, Duplikate, tote Branches |
| **Coverage** | Zeilen- und Branch-Coverage aus JaCoCo |
| **Complexity** | Zyklomatische und kognitive Komplexität pro Datei |

---

## Voraussetzungen

| Was | Wozu |
|---|---|
| **Docker** | SonarQube-Server |
| **Java** im PATH | SonarScanner-CLI |
| Buildbare Quellen | Bytecode für Tiefenanalyse (optional, aber empfohlen) |

---

## Schritt 1 – SonarQube starten

```bash
docker run -d --name sonarqube \
  -p 9000:9000 \
  sonarqube:community
```

**Wichtig:** SonarQube benötigt mindestens **5 GB freien Speicher** auf dem
Host (Elasticsearch-Index). Prüfen mit `df -h /`.

Auf den Start warten (~1–2 Minuten):

```bash
until curl -s http://localhost:9000/api/system/status | grep -q '"status":"UP"'; do
  echo "Warte..."; sleep 5
done && echo "SonarQube ist bereit."
```

---

## Schritt 2 – Admin-Passwort setzen

SonarQube startet mit `admin` / `admin`. Das Passwort **muss** beim ersten
Start geändert werden (Anforderung: min. 12 Zeichen, 1 Großbuchstabe, 1 Zahl).

```bash
MY_PASSWORD="MeinPasswort1!"   # <-- eigenes Passwort wählen und merken

curl -s -u admin:admin \
  -X POST "http://localhost:9000/api/users/change_password" \
  -d "login=admin&previousPassword=admin&password=${MY_PASSWORD}"
```

> **Tipp:** Das Passwort irgendwo notieren. Wenn der Container neu erstellt
> wird, ist es wieder `admin:admin`. Wenn der Container nur neugestartet wird
> (`docker start`), bleibt das Passwort erhalten.

---

## Schritt 3 – Projekt anlegen

```bash
MY_PASSWORD="MeinPasswort1!"   # wie in Schritt 2 gesetzt
PROJECT_KEY="mein-projekt"     # eindeutiger Schlüssel, keine Leerzeichen

curl -s -u "admin:${MY_PASSWORD}" \
  -X POST "http://localhost:9000/api/projects/create" \
  -d "name=${PROJECT_KEY}&project=${PROJECT_KEY}&visibility=private"
```

---

## Schritt 4 – Analyse-Token erzeugen

Der Token ersetzt das Passwort beim Scanner-Aufruf. Er wird nur einmal
angezeigt – sofort sichern.

```bash
MY_PASSWORD="MeinPasswort1!"
PROJECT_KEY="mein-projekt"

curl -s -u "admin:${MY_PASSWORD}" \
  -X POST "http://localhost:9000/api/user_tokens/generate" \
  -d "name=${PROJECT_KEY}-token&type=PROJECT_ANALYSIS_TOKEN&projectKey=${PROJECT_KEY}" \
  | python3 -c "import sys,json; print('Token:', json.load(sys.stdin)['token'])"
```

Den ausgegebenen Token als Umgebungsvariable exportieren:

```bash
export SONAR_TOKEN="sqp_abc123..."
```

---

## Schritt 5 – SonarScanner installieren

```bash
curl -L "https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-7.1.0.4889-linux-x64.zip" \
  -o /tmp/sonar-scanner.zip
unzip -q /tmp/sonar-scanner.zip -d ~/tools/
export PATH="$HOME/tools/sonar-scanner-7.1.0.4889-linux-x64/bin:$PATH"

# Verify:
sonar-scanner --version
```

Dauerhaft in den PATH (einmalig):

```bash
echo 'export PATH="$HOME/tools/sonar-scanner-7.1.0.4889-linux-x64/bin:$PATH"' >> ~/.bashrc
```

---

## Schritt 6 – Projekt analysieren

### 6a – Bytecode bauen (empfohlen)

Bytecode ermöglicht tiefere Analysen (Datenfluss, Null-Checks). Ohne
Bytecode läuft SonarQube im reinen Quelltextmodus.

```bash
# SBT:
sbt --client compile
```

### 6b – Coverage-Report erzeugen (optional)

```bash
# SBT mit scoverage:
sbt --client coverage test coverageReport
# Report: target/scala-3*/scoverage-report/scoverage.xml
```

### 6c – Scanner ausführen

```bash
# Ins Projekt-Root wechseln:
cd /pfad/zum/projekt

sonar-scanner \
  -Dsonar.projectKey="${PROJECT_KEY}" \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token="${SONAR_TOKEN}" \
  -Dsonar.sources=. \
  -Dsonar.inclusions="**/*.scala" \
  -Dsonar.exclusions="**/target/**,**/out/**,**/node_modules/**" \
  -Dsonar.tests=. \
  -Dsonar.test.inclusions="**/*Test.scala,**/*Spec.scala,**/*Suite.scala" \
  -Dsonar.java.binaries=core/target/scala-3.x/classes
```

**Projektspezifische Pfade anpassen:**

| Parameter | SBT / Scala 3 |
|---|---|
| `sonar.inclusions` | `**/*.scala` |
| `sonar.java.binaries` | `<modul>/target/scala-3*/classes` |
| `sonar.coverage.jacoco.xmlReportPaths` | `target/scala-3*/jacoco/report/jacoco.xml` |

Am Ende der Analyse:

```
INFO  ANALYSIS SUCCESSFUL, you can find the results at:
      http://localhost:9000/dashboard?id=mein-projekt
```

---

## Schritt 7 – Ergebnisse anschauen

**http://localhost:9000** → Projects → Projekt anklicken

oder direkt: **http://localhost:9000/dashboard?id=mein-projekt**

### Was man wo findet

```
Dashboard
├── Overview          Zusammenfassung: Quality Gate, alle Metriken auf einen Blick
├── Issues            Alle Bugs / Vulnerabilities / Code Smells mit Zeilenangabe
│   ├── Filter nach Severity: Blocker → Critical → Major → Minor
│   └── Klick auf Issue → Zeile im Code + Erklärung + Link zur Regel
├── Security          Security Hotspots (manuelle Prüfung erforderlich)
├── Measures          Vollständige Metriken-Übersicht
│   ├── Complexity    Zyklomatisch + kognitiv, pro Datei sortierbar
│   ├── Coverage      Zeilen- und Branch-Coverage, welche Zeilen fehlen
│   └── Duplications  Welcher Code ist doppelt vorhanden?
└── Code              Dateibrowser mit Inline-Metriken und markierten Issues
```

### Quality Gate

Das rote/grüne Banner oben zeigt ob das Projekt die definierten
Qualitätsschwellen erfüllt. Standard-Gate prüft:
- 0 neue Bugs
- 0 neue Vulnerabilities  
- Coverage auf neuem Code ≥ 80%
- Duplikate auf neuem Code < 3%

---

## Alles in einem: `run-analysis.sh`

Das Skript in diesem Verzeichnis automatisiert Schritte 3–6:

```bash
# Einmalig: Projekt + Token anlegen
./run-analysis.sh setup

# Token exportieren (Ausgabe von setup kopieren):
export SONAR_TOKEN="sqp_..."

# Analyse ausführen:
./run-analysis.sh scan

# Browser öffnen:
./run-analysis.sh open
```

---

## Server-Lebenszyklus

```bash
# Stoppen (Daten bleiben erhalten):
docker stop sonarqube

# Wieder starten:
docker start sonarqube
# → Passwort ist noch wie gesetzt, alle Projekte und Ergebnisse vorhanden

# Komplett neu (leere Datenbank, Passwort wieder admin:admin):
docker rm -f sonarqube
docker run -d --name sonarqube -p 9000:9000 sonarqube:community
```

---

## Häufige Probleme

| Symptom | Ursache | Lösung |
|---|---|---|
| SonarQube startet nicht | Zu wenig Festplatte | `df -h /` → mind. 5 GB frei; `docker system prune -f` |
| `admin:admin` abgelehnt | Passwort bereits geändert | Gesetztes Passwort verwenden; oder Container neu erstellen |
| Login schlägt fehl obwohl Passwort korrekt | Browser-Cache / alte Session | Inkognito-Fenster oder anderen Browser verwenden |
| `401 Unauthorized` beim Scanner | Token abgelaufen oder falsch | Neues Token: Schritt 4 wiederholen |
| `The folder X does not exist` | Falscher Pfad in `sonar.sources` | Pfade relativ zum Projekt-Root angeben |
| Coverage = 0% | JaCoCo-XML nicht gefunden | Pfad in `sonar.coverage.jacoco.xmlReportPaths` prüfen |
| WARN: `sonar.java.libraries` is empty | Abhängigkeits-JARs fehlen | Für Archaeology-Zwecke harmlos; Analyse läuft trotzdem |
| WARN: `File 'X.kt' not found` | Kotlin-Stdlib im Coverage-Report | Harmlos – betrifft nur externe Bibliotheken |
| Dashboard zeigt keine Daten | Scanner noch nicht gelaufen | URL prüfen: `…/dashboard?id=<project-key>` (exakt wie angelegt) |
