package agents

import upickle.default.*
import sttp.client4.*
import AnthropicModels.*

/** Dünner HTTP-Client für die Anthropic Messages API.
  *
  * Nutzt das synchrone sttp-Backend (`DefaultSyncBackend`, basiert intern auf `java.net.http.HttpClient`) sowie upickle für Serialisierung und Deserialisierung.
  *
  * Authentifizierung erfolgt wie im offiziellen Anthropic-SDK über die Header `x-api-key` und `anthropic-version` (nicht `Authorization: Bearer`).
  */
object AnthropicClient:

  private val BaseUrl          = "https://router.eu.requesty.ai/v1/messages"
  private val AnthropicVersion = "2023-06-01"

  private val apiKey: String =
    Env.get("ANTHROPIC_AUTH_TOKEN").orElse(Env.get("ANTHROPIC_API_KEY"))
      .getOrElse(throw new RuntimeException("Weder ANTHROPIC_AUTH_TOKEN noch ANTHROPIC_API_KEY gesetzt."))

  private val backend = DefaultSyncBackend()

  /** Schreibt eine Log-Zeile für die Ein-/Ausgabe-Kommunikation mit dem Modell auf die Konsole. Zentraler Ort für dieses simple `println`-Logging, damit sich Aufbau und Präfix (`[Anthropic]`) nicht
    * in jeder Methode wiederholen.
    */
  private def log(msg: String): Unit = println(s"[Anthropic] $msg")

  /** Kürzt lange, mehrzeilige Texte für die Log-Ausgabe auf eine einzelne, überschaubare Zeile (Zeilenumbrüche/mehrfache Leerzeichen werden zu einem Leerzeichen zusammengefasst).
    */
  private def truncate(s: String, maxLen: Int = 300): String =
    val flattened = s.replaceAll("\\s+", " ").trim
    if flattened.length > maxLen then flattened.take(maxLen) + "…" else flattened

  /** Führt genau einen Model-Call aus und liefert nur die reinen Text-Blöcke der Antwort (verkettet), analog zu `text_from_message` im Python-Pendant.
    *
    * @param model
    *   Modellname, z. B. `vertex/claude-sonnet-5@eu`
    * @param systemPrompt
    *   Rolle/Instruktion des Agenten
    * @param userMessage
    *   eigentliche Aufgabe/Anfrage
    * @param useWebSearch
    *   ob das server-seitige `web_search`-Tool erlaubt ist
    * @param maxTokens
    *   Obergrenze für die Antwortlänge
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

    val requestBody = write(request)

    log(s"-> Request  model=$model web_search=$useWebSearch")
    log(s"   system:  ${truncate(systemPrompt)}")
    log(s"   user:    ${truncate(userMessage)}")

    val response = basicRequest
      .post(uri"$BaseUrl")
      .header("x-api-key", apiKey)
      .header("anthropic-version", AnthropicVersion)
      .header("content-type", "application/json")
      .body(requestBody)
      .send(backend)

    response.body match
      case Left(errorBody) =>
        log(s"<- Fehler   HTTP ${response.code}: ${truncate(errorBody)}")
        throw new RuntimeException(s"Anthropic API Fehler (HTTP ${response.code}): $errorBody")
      case Right(bodyJson) =>
        val parsed = read[ChatResponse](bodyJson)
        log(s"<- Response stop_reason=${parsed.stop_reason.getOrElse("-")} blocks=${parsed.content.map(_.`type`).mkString(",")}")
        val text   = parsed.content
          .collect { case ContentBlock("text", Some(text), _, _, _, _, _) => text }
          .mkString("\n")
        log(s"   text:    ${truncate(text)}")
        text

  /** Führt einen Tool-Use-Loop mit GENAU EINEM client-seitigen (custom) Tool aus.
    *
    * Ablauf (siehe auch README, Abschnitt "Client-seitiges Tool"):
    *   1. Request mit der Nutzeranfrage + Tool-Definition senden.
    *   2. Antwortet das Modell mit `stop_reason == "tool_use"`, enthält die Antwort einen oder mehrere `tool_use`-Blöcke.
    *   3. Für jeden Block wird der passende Handler aus `toolHandlers` aufgerufen (bekommt das rohe JSON der Eingabeparameter als `ujson.Value`).
    *   4. Die komplette Assistant-Antwort (inkl. `tool_use`-Block) sowie ein `tool_result` pro Aufruf werden als neue Nachrichten angehängt, danach wird erneut gesendet.
    *   5. Wiederholen, bis `stop_reason != "tool_use"` ist.
    *
    * @param toolHandlers
    *   Mapping von Tool-Name -> Funktion, die die rohen JSON-Eingabeparameter (`ujson.Value`) entgegennimmt und einen String (meist JSON) zurückgibt.
    */
  def chatWithTool(
      model: String,
      systemPrompt: String,
      userMessage: String,
      tools: List[ClientTool],
      toolHandlers: Map[String, ujson.Value => String],
      maxTokens: Int = 2000,
  ): String =
    var messages: List[LoopMessage] = List(LoopMessage("user", ujson.Str(userMessage)))
    var turn                        = 0

    log(s"===== Tool-Use-Loop gestartet (model=$model, tools=${tools.map(_.name).mkString(", ")}) =====")
    log(s"   user:    ${truncate(userMessage)}")

    while true do
      turn += 1
      log(s"--- Turn $turn: sende ${messages.size} Nachricht(en) an das Modell ---")

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
        .body(write(request))
        .send(backend)

      val parsed = response.body match
        case Left(errorBody) =>
          log(s"<- Fehler   HTTP ${response.code}: ${truncate(errorBody)}")
          throw new RuntimeException(s"Anthropic API Fehler (HTTP ${response.code}): $errorBody")
        case Right(bodyJson) => read[ChatResponse](bodyJson)

      log(s"<- Turn $turn Antwort: stop_reason=${parsed.stop_reason.getOrElse("-")}")
      parsed.content.foreach { block =>
        block.`type` match
          case "text"     => log(s"   [text]      ${truncate(block.text.getOrElse(""))}")
          case "thinking" => log(s"   [thinking]  ${truncate(block.thinking.getOrElse(""))}")
          case "tool_use" =>
            log(s"   [tool_use]  name=${block.name.getOrElse("?")} id=${block.id.getOrElse("?")} input=${block.input.map(_.render()).getOrElse("{}")}")
          case other      => log(s"   [$other]")
      }

      if !parsed.stop_reason.contains("tool_use") then
        val finalText = parsed.content.collect { case ContentBlock("text", Some(text), _, _, _, _, _) => text }.mkString("\n")
        log(s"===== Tool-Use-Loop beendet nach $turn Turn(s), finale Antwort: ${truncate(finalText)} =====")
        return finalText

      // Die komplette Assistant-Antwort (inkl. tool_use-Blöcken) muss Teil
      // der Historie werden, damit das Modell im nächsten Turn weiß, worauf
      // sich das tool_result bezieht.
      messages = messages :+ LoopMessage("assistant", writeJs(parsed.content))

      val toolResults = parsed.content
        .filter(_.`type` == "tool_use")
        .map { block =>
          val toolName   = block.name.getOrElse("")
          val toolInput  = block.input.getOrElse(ujson.Obj())
          log(s"   >> Tool-Aufruf     $toolName(${toolInput.render()})")
          val handler    = toolHandlers.getOrElse(toolName, (_: ujson.Value) => s"Fehler: kein Handler für Tool '$toolName' registriert.")
          val resultText = handler(toolInput)
          log(s"   << Tool-Ergebnis   $toolName -> ${truncate(resultText)}")
          ToolResultBlock(tool_use_id = block.id.getOrElse(""), content = resultText)
        }

      messages = messages :+ LoopMessage("user", writeJs(toolResults))
    end while
    "" // unreachable, while(true) endet nur per return

  def close(): Unit = backend.close()
