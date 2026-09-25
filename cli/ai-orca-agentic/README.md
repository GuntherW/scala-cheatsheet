# Agentischer Orchestrator mit Orca

Dieselbe Aufgabe wie [`../multi-agent-agentic-orchestrator`](../multi-agent-agentic-orchestrator): ein LLM entscheidet, welche Worker laufen und was parallel darf. Die Ausführung ist danach normaler Code.

Unterschied: Orca steuert die `claude`-CLI. Es gibt keinen Messages-API-Client, keinen Tool-Use-Loop und kein Token-Tracking aus sttp-ai.

## Ablauf

1. **Planner** (`resultAs[PlanDraft]`) liefert Steps und `finalAgentId`.
2. **`PlanValidator`** entfernt unbekannte Ids, ergänzt Pflicht-Agenten und erzwingt `hardDependsOn`.
3. Jeder Step ist eine Stage. Agenten im selben Step laufen über `Par.mapUnordered`.
4. Markdown-Dateien im Worktree.

| Agent | Orca | Pflicht |
|---|---|---|
| Fact-Researcher | `withNetworkOnly` (Websuche des Harness) | nein |
| Risk-Analyst | `withReadOnly`, TCO vorab in Scala (`350 * teamSize + 500`) | nein |
| Synthesis-Agent | `withReadOnly`, bekommt die Worker-Ausgaben | ja |

Ein neuer Agent kommt nur in `AgentRegistry.defs` dazu.

## Start

Voraussetzung: `orca` 0.1.10, JDK 21+, `claude` eingeloggt, dieses Git-Repo. Modell: `vertex/claude-sonnet-5@eu`. `claude.sonnet` lehnt dieser Router ab.

```bash
orca run cli/multi-agent-agentic-orca/report.sc \
  "Sollten wir Kubernetes für unser 5-Personen-Startup einführen?"
```

Der Flow erzwingt einen Worktree. Nicht `--skip-branch` übergeben. Der Abschluss nennt den Pfad, typisch:

```text
.orca/worktrees/<hash>/cli/multi-agent-agentic-orca/output/
```

Aufräumen, zuerst den Worktree:

```bash
git worktree remove .orca/worktrees/<hash>
git branch -D orca-worktree-<hash>
git branch -D <feature-branch>
```

Validator-Tests ohne LLM:

```bash
scala-cli test cli/multi-agent-agentic-orca/PlanValidatorTest.test.scala
```
