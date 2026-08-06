# Software-Archäologie mit code-maat

code-maat (von Adam Tornhill, Autor von *Your Code as a Crime Scene*) analysiert
die **Git-Historie** statt den Quellcode. Es findet Muster im Entwicklungsverhalten:
welche Dateien ändern sich am häufigsten, welche immer zusammen, wer kennt was.

Das macht es zum idealen Werkzeug um **Risikobereiche** zu identifizieren:
> Dateien die *oft geändert* werden *und* groß sind → höchste Änderungswahrscheinlichkeit → größtes Risiko.

## Schnellstart

```bash
cd documentation/archaeology/code-maat
./run-analysis.sh          # Alles: Log → LOC → Analysen → Visualisierung
./run-analysis.sh open     # Visualisierung im Browser öffnen
```

Voraussetzung: **Java** im PATH. Kein Build des Projekts nötig.

## Verzeichnisstruktur

```
code-maat/
├── run-analysis.sh          # Hauptskript
├── tools/
│   ├── code-maat.jar        # code-maat CLI (v1.0.4)
│   ├── merge_hotspots.py    # Hilfsskript: revisions × LOC
│   ├── generate_html.py     # Hilfsskript: HTML-Visualisierung
│   └── hotspot_template.html
├── data/
│   ├── git.log              # Erzeugter Git-Log (wird überschrieben)
│   └── lines.csv            # LOC pro Datei (wird überschrieben)
├── results/                 # CSV-Ergebnisse aller Analysen
│   ├── hotspots.csv         # ⭐ Hotspot-Matrix (revisions × LOC)
│   ├── revisions.csv        # Änderungshäufigkeit pro Datei
│   ├── coupling.csv         # Temporale Kopplung
│   ├── authors.csv          # Autoren pro Datei
│   ├── entity-churn.csv     # Hinzugefügte/gelöschte Zeilen
│   ├── entity-effort.csv    # Aufwand pro Autor und Datei
│   ├── age.csv              # Code-Alter
│   ├── soc.csv              # Sum of Coupling
│   └── summary.csv          # Zusammenfassung
└── visualize/
    └── hotspots.html        # ⭐ Interaktive Visualisierung (sofort öffenbar)
```

## Vorgenerierte Ergebnisse

Die Ergebnisse im `results/`- und `visualize/`-Verzeichnis wurden mit dem
aktuellen Stand des Repositories erzeugt und können **sofort** verwendet werden:

```bash
# Visualisierung direkt öffnen:
xdg-open documentation/archaeology/code-maat/visualize/hotspots.html
```

## Analysen im Detail

### `revisions` – Hotspots (Änderungshäufigkeit)

```bash
java -jar tools/code-maat.jar -l data/git.log -c git2 -a revisions
```

Fragt: *Welche Dateien werden am häufigsten geändert?*

Ausgabe: `entity,n-revs`

**Top-Befunde aus diesem Repository:**

| Datei | Revisionen |
|---|---|
| `application.yml` | 90 |
| `SubmissionProcessor.kt` | 88 |
| `SubmissionProcessorTest.kt` | 72 |
| `SubmissionRepository.kt` | 54 |
| `AdminController.kt` | 40 |

→ `SubmissionProcessor` + `SubmissionRepository` sind die hottest Hotspots.

### `coupling` – Temporale Kopplung

```bash
java -jar tools/code-maat.jar -l data/git.log -c git2 -a coupling
```

Fragt: *Welche Dateien ändern sich immer im selben Commit?*

Ein Kopplungsgrad von 94% zwischen `AdminService.kt` und `AdminServiceTest.kt`
ist normal (Testgetriebene Entwicklung). Verdächtig ist Kopplung zwischen
Dateien, die konzeptuell nichts miteinander zu tun haben.

Ausgabe: `entity,coupled,degree,average-revs`

**Auffällige Kopplung aus diesem Repository:**

| Datei A | Datei B | Grad |
|---|---|---|
| `SecurityConfigDisabledTest` | `SecurityConfigEnabledTest` | 100% |
| `AdminService` | `AdminServiceTest` | 94% |
| `EsapRequestValidationService` | `EsapRequestValidationServiceTest` | 93% |
| `metrics.html` | `submissions.html` | 90% |

→ Die Admin-Templates ändern sich fast immer zusammen: kein Test, aber
  ein Zeichen dass sie inhaltlich eng verzahnt sind.

### `authors` – Autoren pro Datei

```bash
java -jar tools/code-maat.jar -l data/git.log -c git2 -a authors
```

Fragt: *Wer kennt welchen Code?*

Ausgabe: `entity,n-authors,n-revs`

Hohe Autorenzahl = geteiltes Wissen (gut) **oder** zu viele Zuständige (schlecht).
In Kombination mit hohen Revisionen ein Risikosignal.

`application.yml` hat 9 Autoren bei 90 Revisionen → höchste Wissensverteilung,
aber auch potenzielle Konfigurationskonflikte.

### `entity-churn` – Code Churn

```bash
java -jar tools/code-maat.jar -l data/git.log -c git2 -a entity-churn
```

Ausgabe: `entity,added,deleted,commits`

Hoher Churn (viele Zeilen hinzugefügt + gelöscht) bei gleichzeitig vielen
Commits = Code wird ständig umgeschrieben → Instabilität.

### `age` – Code-Alter

```bash
java -jar tools/code-maat.jar -l data/git.log -c git2 -a age
```

Fragt: *Wie lange liegt der letzte Commit auf einer Datei zurück?*

Altes, unberührtes Code = stabil **oder** vergessen. In Kombination mit
hoher Komplexität ein Zeichen für "sleeping giants".

### `soc` – Sum of Coupling

```bash
java -jar tools/code-maat.jar -l data/git.log -c git2 -a soc
```

Aggregiert den Kopplungsgrad einer Datei über alle Paare.
Hoher SoC = zentrale Datei, die viele andere mitzieht.

### `summary` – Überblick

```bash
java -jar tools/code-maat.jar -l data/git.log -c git2 -a summary
```

```
statistic,value
number-of-commits,515
number-of-entities,692
number-of-entities-changed,3351
number-of-authors,10
```

## Hotspot-Matrix (Visualisierung)

Die wichtigste Analyse kombiniert Revisions-Häufigkeit mit Codegröße:

```
Hotspot-Score = Revisions × LOC
```

Öffne `visualize/hotspots.html` im Browser. Die Darstellung zeigt:

- **X-Achse**: Lines of Code (Größe der Datei)
- **Y-Achse**: Anzahl Revisionen (Änderungshäufigkeit)
- **Kreisgröße**: Hotspot-Score (Revisions × LOC)
- **Farbe**: Anzahl Autoren (grün=1, orange=2–4, rot=5+)
- **Rote Zone** oben rechts: höchstes Risiko

## Git-Log neu erzeugen

Der Git-Log wird beim Ausführen von `run-analysis.sh` automatisch neu erzeugt.
Manuell:

```bash
git log --all --numstat --date=short \
    --pretty=format:'--%H--%ad--%aN' \
    -- "service/src" "contract/src" "esap-types/src" \
    > data/git.log
```

Um den Zeitraum einzuschränken:

```bash
# Nur die letzten 6 Monate:
git log --all --numstat --date=short \
    --pretty=format:'--%H--%ad--%aN' \
    --after="6 months ago" \
    -- "service/src" "contract/src" "esap-types/src" \
    > data/git.log
```

## Alle verfügbaren Analysen

| Analyse | Fragt | Nützlich für |
|---|---|---|
| `revisions` | Wie oft geändert? | Hotspot-Identifikation |
| `coupling` | Was ändert sich zusammen? | Versteckte Abhängigkeiten |
| `authors` | Wer kennt was? | Bus-Faktor, Wissensinseln |
| `entity-churn` | Wie viel Code umgeschrieben? | Instabilitätsmessung |
| `entity-effort` | Wer hat wie viel investiert? | Ownership-Analyse |
| `age` | Wann zuletzt angefasst? | Vergessene Bereiche |
| `soc` | Wie stark gekoppelt gesamt? | Zentrale Knotenpunkte |
| `summary` | Überblick | Einstieg |
| `main-dev` | Wer kennt die Datei am besten? | Ansprechpartner finden |
| `communication` | Wer arbeitet zusammen? | Team-Koordinationsbedarf |
| `abs-churn` | Absoluter Churn pro Zeitraum | Trend-Analyse |
