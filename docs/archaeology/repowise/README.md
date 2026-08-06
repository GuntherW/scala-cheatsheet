# Software-Archäologie mit repowise

repowise indexiert ein Git-Repository einmalig und liefert dann sofort
Antworten auf Fragen wie: Welche Dateien sind riskant? Wo ist toter Code?
Welche Klassen sollte man refactoren? Was hängt wovon ab?

Im Unterschied zu den anderen Tools hier kombiniert repowise **statische
Code-Analyse, Git-Historie und Abhängigkeitsgraphen** in einem einzigen,
durchsuchbaren Index – und stellt die Ergebnisse über ein Web-Dashboard,
die Kommandozeile und MCP-Tools für KI-Agenten bereit.

---

## Was repowise analysiert

| Dimension | Was es zeigt |
|---|---|
| **Code Health** | Score 0–10 pro Datei aus 25 Metriken (Komplexität, Verschachtelung, Größe etc.) |
| **Hotspots** | Dateien die oft geändert wurden **und** schlechte Health haben |
| **Refactoring-Kandidaten** | Priorisierte Liste: Impact × Aufwand × Score |
| **Dead Code** | Nicht erreichbare Dateien, ungenutzte Exports |
| **Co-Change** | Welche Dateien ändern sich immer zusammen (versteckte Kopplung) |
| **Abhängigkeitsgraph** | 4.736 Knoten, 10.437 Kanten, Community-Erkennung |
| **Wiki** | 598 automatisch generierte Dokumentationsseiten aus der Codestruktur |
| **Change Risk** | Risiko-Score für einen Diff (0–10) |

---

## Voraussetzungen

- Python 3.10+
- pip

---

## Installation

```bash
pip install repowise
# Bei System-Python:
pip install repowise --break-system-packages

repowise --version   # → repowise, version 0.39.0
```

---

## Schritt 1 – Index erstellen (einmalig)

```bash
cd /pfad/zum/repo

# Kostenlos, kein API-Key, kein LLM:
repowise init --no-prose -y
```

Der Index wird in `.repowise/` gespeichert. Die Analyse dauert ca. 30–60
Sekunden und umfasst:

- **Phase 1 – Ingestion:** Dateien parsen, Abhängigkeitsgraph aufbauen,
  PageRank & Betweenness-Zentralität berechnen, Git-Historie auswerten
- **Phase 2 – Analysis:** Dead Code, Code Health (25 Marker), Architektur-Entscheidungen
- **Phase 3 – Generation:** 598 Wiki-Seiten aus der Codestruktur erzeugen
- **Phase 4 – Persistence:** Index in SQLite-Datenbank speichern

> **Kein API-Key nötig** für den Basis-Index (`--no-prose`).
> Mit `--prose` und einem Anthropic/OpenAI-Key werden die Wiki-Seiten
> von einem LLM verfasst (optionale Zusatzkosten).

---

## Schritt 2 – Dashboard öffnen

```bash
repowise serve
# oder:
./documentation/archaeology/repowise/run-analysis.sh serve
```

Browser öffnen: **http://localhost:3000**

### Was man wo findet

```
http://localhost:3000
├── /health        Code-Health-Karte (Bubble Chart nach Score × Größe)
├── /hotspots      Hotspot-Matrix (Churn × Complexity)
├── /graph         Interaktiver Abhängigkeitsgraph
├── /coupling      Co-Change-Paare (versteckte Kopplung)
├── /dead-code     Nicht erreichbare Dateien und ungenutzte Exports
├── /wiki          598 generierte Dokumentationsseiten
├── /risk          Change-Risk für beliebige Diffs
├── /contributors  Autorenverteilung und Bus-Faktor
├── /decisions     Architekturentscheidungen (aus Code/Commits gemint)
├── /symbols       Alle Symbole mit PageRank und Zentralität
└── /chat          Fragen an den Index stellen (braucht Embedder)
```

---

## Schritt 3 – Kommandozeile

### Code Health

```bash
repowise health
```

Zeigt:
- Gesamt-Score (Hotspot-Score / Durchschnitt / Schlechteste Datei)
- Top-20-Liste der schlechtesten Dateien mit Score, Komplexität (CCN),
  Verschachtelungstiefe (Nest), Zeilenzahl (NLOC), Testabdeckung

**Ergebnis für dieses Repository:**

```
Hotspot: 5.57/10 · Average: 8.59/10 · Worst: 1.0/10 (LeiRecordImporter.kt)
74.3% healthy (429 files) · 22.4% warning (35 files) · 3.4% alert (6 files)

Validierung: 14/20 der schlechtesten Dateien hatten in den letzten 6 Monaten
einen Bug-Fix — 7.3× häufiger als der Durchschnitt (70% vs. 10%).
→ Der Score findet tatsächlich die Problemstellen.
```

### Refactoring-Kandidaten

```bash
repowise health --refactoring-targets
```

Zeigt priorisierte Refactoring-Kandidaten mit:
- **Score** (aktuell)
- **Impact** (wie viel besser wird der Score nach dem Refactoring)
- **Effort** (S/M/L)
- **Ratio** (Impact/Effort – höher = besser)
- **Primary Marker** (Hauptproblem: `nested_complexity`, `untested_hotspot`,
  `co_change_scatter`, `change_entropy` etc.)

Außerdem: konkrete **Extract Helper**-Pläne mit exakten Zeilenangaben wo
Code dupliziert ist und in welche Datei er extrahiert werden sollte.

### Toter Code

```bash
repowise dead-code
```

Findet:
- **Unreachable files** – Dateien die von niemandem importiert werden
- **Unused exports** – Funktionen/Klassen die exportiert aber nie verwendet werden
- **Deletable lines** – Schätzung wie viele Zeilen entfernt werden könnten

**Ergebnis für dieses Repository:**
```
17 nicht erreichbare Dateien · 15 ungenutzte Exports · ~567 löschbare Zeilen
```

### Change Risk

```bash
# Risiko des letzten Commits:
repowise risk HEAD~1..HEAD

# Risiko eines Feature-Branches:
repowise risk main..feature/my-branch
```

Gibt einen Score 0–10 zurück mit Erklärung welche Faktoren das Risiko erhöhen.

---

## Schritt 4 – Index aktuell halten

```bash
# Nach neuen Commits:
repowise update

# Oder automatisch bei jedem Commit (Git-Hook):
repowise hook install
```

---

## Ergebnisse aus diesem Repository

| Metrik | Wert |
|---|---|
| Analysierte Dateien | 468 |
| Symbole | 3.410 |
| Graph-Knoten | 4.736 |
| Graph-Kanten | 10.437 |
| Hotspots (Git) | 36 |
| Durchschnittlicher Health-Score | 8.59/10 |
| Schlechteste Datei | `LeiRecordImporter.kt` (1.0/10) |
| Nicht erreichbare Dateien | 17 |
| Ungenutzte Exports | 15 |
| Wiki-Seiten | 598 |
| Analysezeit | 37 Sekunden |

### Top-Refactoring-Kandidaten

| Datei | Score | Hauptproblem |
|---|---|---|
| `LeiRecordImporter.kt` | 1.0 | `nested_complexity` (CCN 16, 7 Ebenen) |
| `EsapSubmissionRepository.kt` | 2.9 | `untested_hotspot` |
| `FeedbackRepository.kt` | 3.3 | `untested_hotspot` |
| `SubmissionRepository.kt` | 3.6 | `co_change_scatter` |
| `ValidationResult.kt` | 3.9 | `untested_hotspot` |

---

## Optionale Erweiterungen

### Semantische Suche (Ollama, kostenlos)

```bash
# Ollama installieren: https://ollama.com
ollama pull nomic-embed-text

# In .repowise/config.yaml:
# embedder: ollama
```

Danach funktioniert `/chat` im Dashboard – du kannst Fragen an den Code stellen.

### LLM-geschriebene Wiki (mit API-Key)

```bash
export ANTHROPIC_API_KEY="sk-ant-..."
repowise generate          # bestehende Seiten aufwerten
repowise generate --all    # alle 598 Seiten neu schreiben
```

---

## Häufige Probleme

| Problem | Lösung |
|---|---|
| `command not found: repowise` | `pip install repowise --break-system-packages` |
| Dashboard leer nach `serve` | `repowise init` zuerst ausführen |
| Port 3000 belegt | `repowise serve --port 3001` |
| Index veraltet | `repowise update` nach neuen Commits |
| Semantische Suche fehlt | Ollama installieren, `embedder: ollama` in `.repowise/config.yaml` |
