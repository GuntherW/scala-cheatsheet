package agents

import com.github.plokhotnyuk.jsoniter_scala.core.*
import sttp.client4.*
import AnthropicModels.*

/** Dünner HTTP-Client für die Anthropic Messages API.
  *
  * Nutzt das synchrone sttp-Backend (`DefaultSyncBackend`, basiert intern
  * auf `java.net.http.HttpClient`) sowie jsoniter-scala für Serialisierung
  * und Deserialisierung.
  *
  * Authentifizierung erfolgt wie im offiziellen Anthropic-SDK über die
  * Header `x-api-key` und `anthropic-version` (nicht `Authorization:
  * Bearer`).
  */
object AnthropicClient:

  private val BaseUrl = "https://router.eu.requesty.ai/v1/messages"
  private val AnthropicVersion = "2023-06-01"

  private val apiKey: String =
    Env.get("ANTHROPIC_AUTH_TOKEN").orElse(Env.get("ANTHROPIC_API_KEY"))
      .getOrElse(throw new RuntimeException("Weder ANTHROPIC_AUTH_TOKEN noch ANTHROPIC_API_KEY gesetzt."))

  private val backend = DefaultSyncBackend()

  /** Führt genau einen Model-Call aus und liefert nur die reinen
    * Text-Blöcke der Antwort (verkettet), analog zu `text_from_message`
    * im Python-Pendant.
    *
    * @param model       Modellname, z. B. `vertex/claude-sonnet-5@eu`
    * @param systemPrompt Rolle/Instruktion des Agenten
    * @param userMessage  eigentliche Aufgabe/Anfrage
    * @param useWebSearch ob das server-seitige `web_search`-Tool erlaubt ist
    * @param maxTokens    Obergrenze für die Antwortlänge
    */
  def chat(
      model: String,
      systemPrompt: String,
      userMessage: String,
      useWebSearch: Boolean = false,
      maxTokens: Int = 2000,
  ): String =
    val request = ChatRequest(
      model = model,
      max_tokens = maxTokens,
      system = Some(systemPrompt),
      messages = List(ChatMessage(role = "user", content = userMessage)),
      tools = if useWebSearch then Some(List(WebSearchTool())) else None,
    )

    val requestBody = writeToString(request)

    val response = basicRequest
      .post(uri"$BaseUrl")
      .header("x-api-key", apiKey)
      .header("anthropic-version", AnthropicVersion)
      .header("content-type", "application/json")
      .body(requestBody)
      .send(backend)

    response.body match
      case Left(errorBody) =>
        throw new RuntimeException(s"Anthropic API Fehler (HTTP ${response.code}): $errorBody")
      case Right(bodyJson) =>
        val parsed = readFromString[ChatResponse](bodyJson)
        parsed.content
          .collect { case ContentBlock("text", Some(text), _, _, _, _, _) => text }
          .mkString("\n")

  /** Führt einen Tool-Use-Loop mit GENAU EINEM client-seitigen (custom)
    * Tool aus.
    *
    * Ablauf (siehe auch README, Abschnitt "Client-seitiges Tool"):
    *   1. Request mit der Nutzeranfrage + Tool-Definition senden.
    *   2. Antwortet das Modell mit `stop_reason == "tool_use"`, enthält
    *      die Antwort einen oder mehrere `tool_use`-Blöcke.
    *   3. Für jeden Block wird der passende Handler aus `toolHandlers`
    *      aufgerufen (bekommt die rohen JSON-Bytes der Eingabeparameter).
    *   4. Die komplette Assistant-Antwort (inkl. `tool_use`-Block) sowie
    *      ein `tool_result` pro Aufruf werden als neue Nachrichten
    *      angehängt, danach wird erneut gesendet.
    *   5. Wiederholen, bis `stop_reason != "tool_use"` ist.
    *
    * @param toolHandlers Mapping von Tool-Name -> Funktion, die die rohen
    *                     JSON-Eingabeparameter (`RawJson`) entgegennimmt
    *                     und einen String (meist JSON) zurückgibt.
    */
  def chatWithTool(
      model: String,
      systemPrompt: String,
      userMessage: String,
      tools: List[ClientTool],
      toolHandlers: Map[String, RawJson => String],
      maxTokens: Int = 2000,
  ): String =
    var messages: List[LoopMessage] = List(LoopMessage("user", rawJsonString(userMessage)))

    while true do
      val request = LoopChatRequest(
        model = model,
        max_tokens = maxTokens,
        system = Some(systemPrompt),
        messages = messages,
        tools = Some(tools),
      )

      val response = basicRequest
        .post(uri"$BaseUrl")
        .header("x-api-key", apiKey)
        .header("anthropic-version", AnthropicVersion)
        .header("content-type", "application/json")
        .body(writeToString(request))
        .send(backend)

      val parsed = response.body match
        case Left(errorBody) =>
          throw new RuntimeException(s"Anthropic API Fehler (HTTP ${response.code}): $errorBody")
        case Right(bodyJson) => readFromString[ChatResponse](bodyJson)

      if !parsed.stop_reason.contains("tool_use") then
        return parsed.content.collect { case ContentBlock("text", Some(text), _, _, _, _, _) => text }.mkString("\n")

      // Die komplette Assistant-Antwort (inkl. tool_use-Blöcken) muss Teil
      // der Historie werden, damit das Modell im nächsten Turn weiß, worauf
      // sich das tool_result bezieht.
      messages = messages :+ LoopMessage("assistant", RawJson(writeToArray(parsed.content)))

      val toolResults = parsed.content
        .filter(_.`type` == "tool_use")
        .map { block =>
          val toolName = block.name.getOrElse("")
          val handler = toolHandlers.getOrElse(toolName, (_: RawJson) => s"Fehler: kein Handler für Tool '$toolName' registriert.")
          val resultText = handler(block.input.getOrElse(RawJson("{}".getBytes)))
          ToolResultBlock(tool_use_id = block.id.getOrElse(""), content = resultText)
        }

      messages = messages :+ LoopMessage("user", RawJson(writeToArray(toolResults)))
    end while
    "" // unreachable, while(true) endet nur per return

  def close(): Unit = backend.close()
