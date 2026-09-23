# Agentischer, generischer Multi-Agenten-Orchestrator (Scala 3.9.0 / scala-cli / kyo-ai)

Dies ist die **kyo-ai-Variante** des Lern-/Lehrbeispiels aus [`cli/multi-agent-agentic-orchestrator`](../multi-agent-agentic-orchestrator)
(sttp-ai). Gleicher fachlicher Umfang, gleiche CCAF-Terminologie und Domänen-Konzepte - andere LLM-Bibliothek
([kyo-ai](https://github.com/getkyo/kyo/tree/main/kyo-ai), Teil des [Kyo](https://getkyo.io)-Toolkits). Diese README
konzentriert sich auf die Unterschiede zum sttp-ai-Original; für die ausführliche CCAF-Begriffserklärung siehe dort.

## Tech-Stack

| Baustein | Bibliothek |
|---|---|
| LLM-Integration (Agent-Loop, Structured Output, Tools) | [`kyo-ai`](https://github.com/getkyo/kyo/tree/main/kyo-ai) `1.0.0-RC6` |
| Effekt-System / Nebenläufigkeit (`Async.foreach`, `LLM.run`) | [`kyo-core`](https://github.com/getkyo/kyo/tree/main/kyo-core) (transitiv über `kyo-ai`) |
| HTTP-Client für den handgerollten `web_search`-Pfad | [`kyo-http`](https://github.com/getkyo/kyo/tree/main/kyo-http) |
| JSON-Encode/Decode außerhalb von `AI.gen` | [`kyo-schema-json`](https://github.com/getkyo/kyo/tree/main/kyo-schema-json) |
| Dateizugriff (`.env`, `output/`) | [`os-lib`](https://github.com/com-lihaoyi/os-lib) |
| Tests | [MUnit](https://scalameta.org/munit/) |

## Warum kyo-ai statt sttp-ai? Was ändert sich dadurch?

kyo-ai verfolgt einen deklarativeren Ansatz: ein LLM-Aufruf ist ein **typisierter Wert** (`A < LLM`), den man komponiert,
statt eines Requests, den man orchestriert. Das räumt viel Boilerplate ab, die im sttp-ai-Original noch von Hand
geschrieben werden musste:

| Baustein im sttp-ai-Original | Ersatz in dieser kyo-ai-Variante |
|---|---|
| `Agent.scala` (abstrakte Basisklasse, `require`-Invariante) | `Agents.scala` (freie Funktionen `run`/`runStructured`) |
| `AnthropicClient.buildAgent`/`buildStructuredAgent` (sttp-ai `ClaudeAgent`-Fabrik) | `LlmConfig.config` (`AI.Config`) + `AI.gen[T]`/`AI.enable` |
| `JsonExtraction.parseLenient` (manuelles JSON-Parsing für Worker-Reports) | **entfällt** für alle `AI.gen[T]`-Aufrufe (Structured Output erzwingt das Schema bereits API-seitig) - bleibt nur für den `web_search`-Sonderfall |
| `CalculateTcoTool.scala` (manuelles JSON-Schema + `Map[String, Json]`-Handler) | `Tool.init[CalculateTcoInput](...)` - Schema wird aus dem Case-Class-Typ abgeleitet |
| `Interceptors.scala` (`LoggingInterceptor`/`UsageTrackingInterceptor`/`BudgetInterceptor`) | `Observability.scala` (`Observe`-Enablements + `kyo.Log` + selbstgebauter `Abort`-Guard) |
| `ox.par` (strukturierte Nebenläufigkeit für Fan-out/Fan-in pro Step) | `kyo.Async.foreach` |
| `PlanValidator.scala` | **unverändert** (reine, LLM-freie Logik - keine Berührung mit `AI`/`LLM`) |

## Der eine echte Stolperstein: server-seitiges `web_search`

`kyo-ai`s Tool-Abstraktion (`Tool.init`/`AI.enable`) baut jedes Tool intern als `internal.Info[?, ?, LLM]` mit einem
**lokalen Round-Trip**: das Modell ruft das Tool auf, WIR führen die `run`-Funktion aus, das Ergebnis geht zurück ans
Modell. Anthropics `web_search_20250305` ist aber ein **Server-Tool**: der Server löst die Suche komplett selbst
innerhalb EINES HTTP-Response auf - es gibt nie einen lokal zu beantwortenden Tool-Call. Es gibt in kyo-ai (Stand
`1.0.0-RC6`) **keinen öffentlichen Weg**, ein solches providernatives Server-Tool in den Tool-Katalog eines
`AI.gen`-Aufrufs einzuschleusen.

Für den `Fact-Researcher` bypassen wir daher `LLM`/`AI.gen` komplett und sprechen `POST /v1/messages` direkt über
`kyo-http` an (`WebSearchClient.scala`) - exakt dieselbe Notlösung, die das sttp-ai-Original
(`AnthropicClient.webSearchAgent`) aus demselben strukturellen Grund brauchte. Die Antwort wird lokal decodiert
(`JsonExtraction.parseLenient`, mit Markdown-Codefence-Stripping), da dieser Pfad kein natives Structured Output
erzwingen kann.

Alle anderen Agenten (`Planner`, `Risk-Analyst`, `Synthesis-Agent`) laufen ganz normal über `AI.gen`/`LLM.run`.

## Die Agenten & die generische Registry

| Agent | Tool | Abhängigkeiten | Pflicht | Ergebnistyp |
|---|---|---|---|---|
| `Fact-Researcher` | `web_search` (handgerollt, siehe oben) | keine | nein | `FactReport` (JSON) |
| `Risk-Analyst` | `calculate_tco` (`Tool.init`, client-seitig) | keine | nein | `RiskReport` (JSON) |
| `Synthesis-Agent` | keins | Fact-Researcher, Risk-Analyst | ja | Freitext-Bericht |

Ein neuer Agent wird eingebunden, indem lediglich eine neue `AgentSpec`-Instanz in `AgentRegistry.specs` ergänzt wird -
weder `Orchestrator` noch `AgentPlanner` müssen dafür angepasst werden (siehe `AgentSpec.scala`).

## Ablauf

1. **Planning** (agentisch): `AgentPlanner.plan` lässt das Modell selbst entscheiden, welche Agenten in welcher
   Reihenfolge/Parallelität laufen sollen (`AI.gen[ExecutionPlan]`, Structured Output).
2. **Validierung** (rein, LLM-frei): `PlanValidator.validate` erzwingt strukturelle Korrektheit (Abhängigkeiten,
   Pflicht-Agenten, gültige `finalAgentId`) via stabilem Kahn-Toposort - unverändert gegenüber dem Original.
3. **Execution** (generisch): `Orchestrator.runPipeline` führt die validierten Steps aus; Agenten innerhalb eines
   Steps laufen parallel über `Async.foreach`, der nächste Step startet erst nach vollständigem Abschluss des
   aktuellen.

Alle drei Agentenergebnisse werden als kompaktes JSON zwischen den Agenten weitergereicht (`Json.encode`/`derives
Schema`), nicht als Freitext ("Upstream Agent Optimisation", siehe Original-README) - nur der `Synthesis-Agent` am
Ende liefert Freitext (Markdown) für Menschen.

## Fehlerbehandlung (CCAF 5.3 "Error Propagation")

Wie im Original wird ein "unsauberes" Ende eines Model-Calls (Transport-Fehler, Budget überschritten, Decode-Fehler,
ausgeschöpfte Eval-Loop-Iterationen) NICHT als regulärer Erfolg getarnt (Anti-Pattern "Silent Suppression"):
`AgentRunOutcome`/`ReportOutcome` (`AgentTypes.scala`) unterscheiden explizit zwischen `Success` und
`PartialFailure` samt `failureType` (`Transient`/`Validation`). Der `Synthesis-Agent` bekommt das transparent
mitgeteilt und muss es im finalen Bericht als "Datenabdeckung"-Abschnitt ausweisen statt Lücken zu kaschieren.

## Observability: Logging, Usage-Tracking, Budget-Guard

Ersatz für die drei sttp-ai-Interceptoren (`Observability.scala`), aufgebaut auf kyo-ais `Observe`-Enablement
(wire-tier Turn-Benachrichtigung, siehe kyo-ai-README "Tracking usage"):

- **Logging**: `Observe.init` + `kyo.Log.info` protokolliert jeden abgeschlossenen Model-Turn.
- **Usage-Tracking**: ein zweites `Observe` sammelt Tokens/Modell pro Aufruf in einem pipeline-weiten,
  nebenläufigkeitssicheren `UsageCollector` - der Abschlussbericht (`output/99_usage_report.md`) listet Tokens/Kosten
  pro Agent und in Summe.
- **Budget-Guard**: ein drittes `Observe` summiert Tokens EINES Agenten-Aufrufs über einen `AtomicRef` und löst bei
  Überschreitung eines generösen Limits (`Observability.perCallTokenBudget`, 200.000 Tokens) `Abort.when(...)` mit
  einem eigenen `BudgetExceeded`-Fehlertyp aus - Pattern direkt aus dem kyo-ai-README (Abschnitt "Tracking usage",
  Beispiel `capped`).

## Bekannter Stolperstein: Reasoning + großer Kontext + Requesty-Router

Bei der Entwicklung zeigte sich: Mit `disableReasoning` liefert das Modell (`vertex/claude-sonnet-5@eu` über
`router.eu.requesty.ai`) bei einem größeren Input-Kontext (die beiden JSON-Reports im Synthesis-Prompt) wiederholt
eine nicht schema-konforme Antwort über den von kyo-ai erzwungenen "Result-Tool"-Mechanismus
(`AIEvalExhaustedException` nach 5 Iterationen + Repair-Turn). Mit aktivem Reasoning (kyo-ai-Default) gelingt
derselbe Aufruf zuverlässig öfter, aber nicht immer: gelegentlich (wenn das Modell die erzwungene Tool-Antwort im
ersten Versuch verfehlt und kyo-ai einen internen "Repair-Turn" mit Assistant-Message-Prefill einleitet) lehnt dieser
Router/dieses Modell den Request mit `"This model does not support assistant message prefill"` ab - ein bekanntes
Zusammenspiel aus aktivem Reasoning und erzwungenem `tool_choice` (siehe kyo-ai-README, Abschnitt "Reasoning": "two
endpoints refuse a forced tool call while reasoning is active"). `LlmConfig.config` setzt daher `retrySchedule(2)`
als Abfederung; tritt der Fehler dennoch auf, degradiert die Pipeline (wie oben beschrieben) zu einem transparent
gekennzeichneten `PartialFailure`-Bericht statt hart abzustürzen - ein Router-/Modell-spezifischer Stolperstein
analog zu den im sttp-ai-Original dokumentierten (siehe dortige README, Abschnitt "Stolpersteine mit sttp-ai").

## Projektstruktur

```
AgentTypes.scala           FailureType, ReportStatus, AgentRunOutcome, ReportOutcome
AgentSpec.scala            generischer Agenten-Knoten (id, deps, mandatory, execute)
AgentRegistry.scala        zentrale Liste aller Agenten
ExecutionPlan.scala        Structured-Output-Ergebnis der Planungsphase
FactReport.scala / RiskReport.scala   strukturierte Worker-Ergebnisse
LlmConfig.scala             AI.Config (Requesty-Router, Modell, Key aus .env) + .env-Parser
Agents.scala                 run/runStructured (Ersatz für die Agent-Basisklasse)
Observability.scala        Observe-basiertes Logging/Usage-Tracking/Budget-Guard
WebSearchClient.scala       handgerollter web_search-Einzel-Request-Pfad (kyo-http)
JsonExtraction.scala        lenientes JSON-Parsing NUR für WebSearchClient
CalculateTcoTool.scala      Tool.init-basiertes Dummy-Tool
AgentFactResearcher.scala / AgentRiskAnalyst.scala / AgentSynthesis.scala / AgentPlanner.scala
PlanValidator.scala         reine Plan-Validierung/-Reparatur (unverändert)
Orchestrator.scala          zwei-Phasen-Koordination (Planning, Execution)
Main.scala                  KyoApp-Einstiegspunkt
```

## Ausführen

```bash
scala-cli run . -- "Sollten wir für ein 3-Personen-Team Redis oder Memcached einsetzen?"
scala-cli test .
```

Ergebnisse landen in `output/` (pro Agent eine Datei, `99_final_report.md`, `99_usage_report.md`).

## Credentials

`.env` im Projektordner (git-ignoriert), analog zum Original:

```
ANTHROPIC_API_KEY="..."
```

Alternativ `ANTHROPIC_AUTH_TOKEN` oder eine echte Umgebungsvariable.

## Gotchas

- JDK 21+ erforderlich (kyo-core-Anforderung, siehe kyo-README).
- kyo verlangt bestimmte Compiler-Flags (`-Wvalue-discard`, `-Wnonunit-statement`,
  `-Wconf:msg=(unused.*value|discarded.*value|pure.*statement):error`, `-language:strictEquality`) - bereits in
  `project.scala` gesetzt.
- `.sc`-Skriptdateien werden von kyo-`KyoApp` NICHT unterstützt (scala-cli wrapped `.sc`-Inhalte in ein Synthetik-
  Objekt, das mit `object X extends KyoApp` kollidiert) - alle Quelldateien dieses Projekts sind daher reguläre
  `.scala`-Dateien.
- `AnthropicCompletion` (kyo-ai) hängt selbst nur `/messages` (ohne `/v1`) an `apiUrl` an - `LlmConfig.config` setzt
  daher `apiUrl(s"$baseUrl/v1")`, während `WebSearchClient` den vollen Pfad `$baseUrl/v1/messages` selbst
  zusammensetzt.
