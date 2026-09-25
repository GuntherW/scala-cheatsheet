# Entscheidungsbericht mit Orca

Gleiche Aufgabe wie [`../multi-agent-manual-orchestrator`](../multi-agent-manual-orchestrator): zu einem Thema einen ausgewogenen Bericht schreiben. Andere Schicht. Orca steuert die `claude`-CLI. Es spricht nicht die Anthropic Messages API.

## Ablauf

1. **Fact-Researcher** und **Risk-Analyst** parallel (`Par.mapUnordered`).
2. **Synthesis-Agent**, sobald beide fertig sind.
3. Drei Markdown-Dateien im Worktree.

| Agent | Orca | Entspricht im Manual-Projekt |
|---|---|---|
| Fact-Researcher | `claude` mit `vertex/claude-sonnet-5@eu`, `withNetworkOnly` | server-seitiges `web_search` |
| Risk-Analyst | dasselbe Modell, `withReadOnly`, plus vorab gerechnetes JSON | client-seitiges `calculate_tco` |
| Synthesis | dasselbe Modell, `withReadOnly`, kein Tool | Aggregator ohne Tool |

`calculate_tco` ist hier eine Scala-Funktion (`CalculateTco.estimate`, Formel `350 * teamSize + 500`, Teamgröße 5). Das Modell ruft sie nicht auf. Orca hat keinen Tool-Use-Loop.

Schlägt ein Worker fehl, bricht `Par.mapUnordered` den anderen laufenden Worker ab. Beide teilen sich eine Stage: ein Resume wiederholt beide, nicht nur den gescheiterten. Synthesis ist eine eigene Stage.

## Start

Voraussetzung: `orca` 0.1.10, JDK 21+, `claude` eingeloggt, dieses Git-Repo. Nicht der Requesty-Key aus `.env`. Das Modell ist `vertex/claude-sonnet-5@eu` — `claude.sonnet` (`claude-sonnet-5`) lehnt dieser Router ab.

```bash
orca run cli/multi-agent-manual-orca/report.sc \
  "Sollten wir Kubernetes für unser 5-Personen-Startup einführen?"
```

Der Flow steht nicht in `orca list` (nur eingebaute und abgelegte Flows). Der Pfad reicht. Ohne Prompt gilt dasselbe Default-Thema wie im Manual-Projekt.

Ohne `orca`-Binary, direkt über scala-cli:

```bash
scala-cli run --workspace "$(mktemp -d)" cli/multi-agent-manual-orca/report.sc -- \
  "Sollten wir Kubernetes für unser 5-Personen-Startup einführen?"
```

Der Flow setzt `RunTarget.Worktree` selbst. Der aktuelle Checkout wechselt keinen Branch. Nicht `--skip-branch` übergeben (committet auf den aktuellen Branch, und die Kombination mit Worktree lehnt Orca ab).

Die Berichte liegen im Worktree, nicht im Arbeitsverzeichnis:

```text
.orca/worktrees/<hash>/cli/multi-agent-manual-orca/output/
```

Der Abschluss von Orca nennt den Pfad.

Gleicher Prompt noch einmal: fertige Stages werden übersprungen. Für einen frischen Lauf Worktree und Branches löschen (Orca räumt sie nicht auf):

```bash
git worktree remove .orca/worktrees/<hash>
git branch -D orca-worktree-<hash>
git branch -D <feature-branch>
```

Zuerst den Worktree entfernen, dann die Branches. Die Feature-Branch heißt `decision-report-…` (Slug des Themas).

## Was dieser Flow nicht zeigt

- Messages-API, Requesty-Router, `stop_reason = tool_use`
- Orcas Review- und PR-Schleife
