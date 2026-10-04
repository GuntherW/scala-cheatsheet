package agents

import kyo.*

/** Handgerollter Einzel-Request-Aufruf gegen die Anthropic Messages API mit dem server-seitigen Tool `web_search_20250305`.
  *
  * '''Warum nicht einfach `Tool.init`/`AI.enable`?''' kyo-ai bindet jedes über `AI.enable`/`Tool.init` registrierte Tool intern als `internal.Info[?, ?, LLM]` mit lokalem Round-Trip in das jeweilige
  * `Completion`-Backend ein: Das Modell ruft das Tool auf (`tool_use`), WIR führen die `run`-Funktion lokal aus und senden das Ergebnis zurück - der Eval-Loop kümmert sich um diesen Hin-und-Her.
  * `web_search_20250305` ist aber ein Anthropic-'''Server'''-Tool: Der Server löst die Suche komplett selbst innerhalb EINES HTTP-Response auf, es gibt nie einen lokal zu beantwortenden Tool-Call
  *   - strukturell also das Gegenteil dessen, was `Tool[S]`/`AI.gen`'s Eval-Loop erwartet. Es gibt in `kyo-ai` (Stand 1.0.0-RC7) keinen öffentlichen Weg, ein solches providernatives Server-Tool in
  *     den Tool-Katalog eines `AI.gen`-Aufrufs einzuschleusen.
  *
  * Für dieses eine Tool bypassen wir daher den `LLM`-Effekt komplett und sprechen `POST /v1/messages` direkt über `kyo-http` an - exakt dieselbe Notlösung, die das sttp-ai-Original
  * (`AnthropicClient.webSearchAgent`) aus demselben strukturellen Grund brauchte (siehe dort). Weil dieser Pfad außerhalb von `LLM`/`AI.gen` läuft, profitiert er NICHT von `Observe`/Budget-Guard
  * (siehe `Observability.scala`) - `AgentFactResearcher` loggt daher selbst grob, siehe `research`.
  */
object WebSearchClient:

  // Bewusst snake_case Feldnamen (statt der Projekt-Konvention camelCase): dies sind reine Wire-Format-DTOs für die
  // Anthropic Messages API, deren JSON-Keys (`max_tokens`, `stop_reason`, ...) exakt so heißen müssen - kyo-schema
  // serialisiert Case-Class-Feldnamen 1:1 als JSON-Keys, ohne implizite Namenskonvertierung.
  private case class ContentBlock(`type`: String, text: Option[String]) derives Schema
  private case class MessagesResponse(content: List[ContentBlock], stop_reason: Option[String]) derives Schema
  private case class ToolSpec(`type`: String, name: String) derives Schema
  private case class WireMessage(role: String, content: String) derives Schema
  private case class MessagesRequest(
      model: String,
      max_tokens: Int,
      system: String,
      messages: List[WireMessage],
      tools: List[ToolSpec],
  ) derives Schema

  /** Führt EINEN Request gegen `POST {baseUrl}/v1/messages` mit dem Tool `web_search_20250305` aus und liefert den zusammengefügten Text aller `text`-Content-Blocks der Antwort (server-seitige
    * Suchergebnisse/Zwischenschritte wie `server_tool_use`/`web_search_tool_result` werden vom Server selbst aufgelöst und erscheinen hier nicht als eigene Blocktypen, die wir behandeln müssten - uns
    * interessiert nur der finale Text).
    */
  def research(
      baseUrl: String,
      apiKey: String,
      model: String,
      systemPrompt: String,
      userMessage: String,
      maxTokens: Int,
  ): String < (Async & Abort[HttpException]) =
    val request = MessagesRequest(
      model = model,
      max_tokens = maxTokens,
      system = systemPrompt,
      messages = List(WireMessage(role = "user", content = userMessage)),
      tools = List(ToolSpec(`type` = "web_search_20250305", name = "web_search")),
    )
    HttpClient
      .withConfig(_.timeout(90.seconds))(
        HttpClient.postJson[MessagesResponse](
          s"$baseUrl/v1/messages",
          request,
          headers = Seq("x-api-key" -> apiKey, "anthropic-version" -> "2023-06-01"),
        ),
      )
      .map(resp => resp.content.flatMap(_.text).mkString("\n"))
