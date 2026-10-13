package agents

import kyo.*

/** Zentrale LLM-Konfiguration: liefert das `AI.Config` (Requesty-Router, Modell, Key aus `sys.env`), das alle Agenten (siehe `Agents.scala`) über `LLM.run` nutzen, sowie die rohen Werte (`baseUrl`,
  * `apiKey`, `model`), die `WebSearchClient` für den handgerollten `web_search`-Pfad braucht (siehe dort).
  *
  * Dieses Projekt spricht (wie das sttp-ai-Original) einen requesty.ai-Router statt der offiziellen Anthropic-Basis-URL an - `AI.Config.Anthropic.default.apiUrl(...)` unterstützt das direkt (Standard
  * wäre `https://api.anthropic.com`).
  */
object LlmConfig:

  /** Basis-URL für `WebSearchClient` (siehe dort), das selbst `$baseUrl/v1/messages` zusammensetzt - OHNE das abschließende `/v1`. */
  val baseUrl: String = "https://router.eu.requesty.ai"

  val model: String = "vertex/claude-sonnet-5-5@eu"

  val apiKey: String = sys.env("ANTHROPIC_API_KEY")

  // kyo-ais AnthropicCompletion-Backend haengt selbst nur "/messages" (ohne "/v1") an `apiUrl` an - anders als
  // WebSearchClient (siehe dort), das den vollen Pfad selbst zusammensetzt. Damit beide denselben Router treffen,
  // MUSS `apiUrl` hier bereits das "/v1"-Segment enthalten.
  //
  // Reasoning bleibt bewusst AN (kyo-ai-Default): Ein Test mit `disableReasoning` gegen genau diesen Router zeigte,
  // dass das Modell bei einem groesseren Input-Kontext (z. B. die beiden JSON-Reports im Synthesis-Prompt, siehe
  // `AgentSynthesis.scala`) wiederholt eine nicht schema-konforme Antwort ueber den erzwungenen "Result-Tool"-
  // Mechanismus lieferte (`AIEvalExhaustedException` nach 5 Iterationen + Repair-Turn) - mit aktivem Reasoning
  // gelang derselbe Aufruf sofort. Ein Stolperstein dieses spezifischen Routers/Modells, analog zu den im
  // sttp-ai-Original dokumentierten Router-Eigenheiten (siehe README).
  val config: AI.Config = AI.Config.Anthropic.default
    .apiUrl(s"$baseUrl/v1")
    .apiKey(apiKey)
    .modelName(model)
    .timeout(90.seconds)
    .retrySchedule(Schedule.repeat(2))
