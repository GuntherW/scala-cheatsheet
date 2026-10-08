# Ziel

kyo-Idiomatik des Ports cli/multi-agent-agentic-orchestrator-kyo gegen die kyo-ai-Doku
(https://getkyo.io/latest/kyo-ai/) bewerten und konkrete Verbesserungen vorschlagen.

## Erkenntnisse & Architektur

- Code: Agents.scala (run/runStructured, toOutcome), Observability.scala (Observe-Init + AtomicRef-Budget nach
  Doku-Beispiel `capped`), LlmConfig.scala (Env + AI.Config.Anthropic.default…modelName), Orchestrator.scala
  (Async.foreach Fan-Out), WebSearchClient.scala (roher kyo-http-POST, bewusster Bypass).
- Verifizierte API-Fakten (cellar, kyo 1.0.0-RC6):
    - `AI.enable[A,S](enablements: Enablement[S]*)(v)` UND `Seq`-Overload existieren → `Agents.enablements` (Seq) OK.
    - `Prompt.init(prompt: => String < LLM & S, reminder: => String < LLM & S)` → effectful prompts möglich; `andThen`/
      `empty`.
    - `Observe.init(f: (AI, Completion.Reply) => Unit < LLM & Sync & S)`; `Observe.withStats(v)` / `withStats(ais*)` →
      (AIStats, A).
    - `AIStats.add(that)`, `AIStats.empty`, `totalTokens`, `inputTokens`, `outputTokens`,
      `cachedInputTokens/reasoningOutputTokens` als Maybe.
    - `Log.info/warn(msg: => String)`.
    - `AI.Config.Anthropic.default` PURE (nur `AI.Config.default` ist effectful) → LlmConfig korrekt.
    - `modelName(...)` offiziell für Proxy-Alias-Wiederverwendung → LlmConfig (vertex/claude-sonnet-5-5@eu) korrekt.
    - Doku bestätigt: Reasoning default ON; Reasoning-Aktivierung + forced tool_choice → von 2 Endpoints abgelehnt →
      unser `.retrySchedule(Schedule.repeat(2))`-Mitigations-Pfad bestätigt.

## Bereits konform (kyo-idiomatisch)

- Typed Results `AI.gen[T]` (runStructured), `AI.gen[String]` (run), Schema-abgeleitete DTOs (`derives Schema`).
- Ein `AI.enable` mit Prompt+Tools im MIX (Agents.enablements) statt verschachtelter enable-Blöcke (Doku "Composing
  binders").
- Budget-Guard exakt nach Doku-`capped`-Beispiel (AtomicRef.init + Observe.init + Abort.when +
  Abort.run[BudgetExceeded]).
- `LLM.run(LlmConfig.config.maxTokens(...))`-explizite-Config-Grenze; `KyoApp`-Main; `Async.foreach` strukturierte
  Nebenläufigkeit im Orchestrator.

## Bewusste Abweichungen (dokumentiert, KEINE Findings)

- `UsageCollector`-Singleton (global mutable) statt getelter `Ref` — pragmatisch wegen generischem `AgentSpec.execute`,
  in README/AGENTS dokumentiert.
- WebSearchClient-Bypass (server-seitiges Tool, kyo-ai kann das nicht) — dokumentiert.
- Observer mit `AI.config.map(...)` im Callback → Doku-konform.

## Kandidaten für idiomatischere/konsistentere Refactors (User-Entscheide)

- [x] A) `Agents.run`/`runStructured`: gemeinsamer `discharge`-Helper (ein Fehlerklassifikations-Ort); `toOutcome`
  entfernt; `alternativeApproaches` als gemeinsames `val` in runStructured.
- [x] B) `UsageCollector.report`: `Totals(stats, cost)` + `totalsOf` (ein Fold mit `AIStats.add`) statt 3x
  Traversierung.
- [x] C) `safeMessage(error)` in AgentTypes.scala (`Option(getMessage).getOrElse(class.getSimpleName)`); genutzt in
  Agents.run/runStructured + AgentFactResearcher.rawOutputSnippet.
- [x] D) `Env.require` (LlmConfig.scala) entfernt; nur `Env.get` bleibt.
- [x] E) `p`-Interpolator für ALLE 5 Multi-Line-Prompts (AgentPlanner.systemPrompt, AgentFactResearcher.systemPrompt,
  AgentRiskAnalyst.systemPrompt, AgentSynthesis.systemPrompt, AgentSynthesis.synthesize-userMessage). Definiziert als
  `extension (sc: StringContext) def p(args: Any*): String = sc.s(args*).replaceAll("\n\\s+", "\n").trim` in
  kyo.Prompt.scala; via `import kyo.*` verfügbar (PTest.scala verifiziert: Leerzeilen kollabieren, Zeilen-Whitespace
  wird gestrippt — vom User akzeptiert).
- [ ] F) Berichts-Nuance: Report-"total=input+output" ignoriert cached/reasoning (AIStats.totalTokens inkludiert sie) —
  bewusst so? ggf. kommentieren.

## Was nach der p-Umstellung angepasst werden musste

- Edit auf `object AgentFactResearcher`/`object AgentSynthesis` hatte die Member (private val systemPrompt / def …) auf
  Top-Level dedentiert („Not found: name") → beide Dateien per `write` komplett neu erstellt, danach `scala-cli fmt .`/
  `compile`/`test` grün, 13/13.
- Achtung bei künftigen Edits: bei multi-line `p"""..."""` die Anfangszeile mit Objekt-Indent + `|`-Marker nie
  weglassen.

## Checkliste

- [x] kyo-ai-Doku vollständig gelesen
- [x] alle ~20 Projektdateien gelesen
- [x] API-Signaturen via cellar verifiziert (Prompt/AI.enable/Observe/AIStats/Log/Config)
- [x] User-Frage beantwortet: A+B+C+D+E ja, F bleibt offen
- [x] A–E umgesetzt, fmt/compile/test grün (13/13)
- [ ] optional: kompletter E2E-Lauf zur Prompt-Verhaltens-Kontrolle mit p-Format