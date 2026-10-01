package agent

import io.circe.Json
import sttp.ai.claude.ClaudeSyncClient
import sttp.ai.claude.config.ClaudeConfig
import sttp.ai.claude.models.{ContentBlock, Message, Tool}
import sttp.ai.claude.requests.MessageRequest
import sttp.model.Uri
import ox.timeout
import sttp.ai.claude.responses.MessageResponse

import scala.concurrent.duration.*

/** Dünner Wrapper um den `ClaudeSyncClient` aus [[https://sttp-ai.softwaremill.com/ sttp-ai]] (Modul `claude`), analog zu `ai-sttpai-manual/AnthropicClient.scala` - mit einer zentralen Ergänzung: Der
  * Tool-Use-Loop hält hier nicht eine FESTE `tools`-Liste über alle Turns hinweg, sondern eine, die WÄCHST, sobald ein Tool-Aufruf neue Tools "freischaltet" (`ToolCallResult.enables`, siehe
  * `ToolCatalog.scala`). Wichtig: Dieser Client kennt `search_tools` NICHT als Sonderfall - er behandelt jeden Tool-Aufruf identisch (`executeToolUse`) und reagiert nur generisch auf `enables`. Das
  * Tool-Search-Paradigma selbst "passiert" ausschließlich in `ToolCatalog.scala`.
  */
object AnthropicClient:

  private val AnthropicVersion = "2023-06-01"

  private val apiKey: String = Env.get("ANTHROPIC_AUTH_TOKEN")
    .orElse(Env.get("ANTHROPIC_API_KEY"))
    .getOrElse(sys.error("Weder ANTHROPIC_AUTH_TOKEN noch ANTHROPIC_API_KEY gesetzt."))

  private val config = ClaudeConfig(
    apiKey = apiKey,
    anthropicVersion = AnthropicVersion,
    baseUrl = Uri.unsafeParse("https://router.eu.requesty.ai"),
  )

  private val client = ClaudeSyncClient(config)

  private val RequestTimeout = 60.seconds

  private def log(msg: String): Unit = println(s"[Agent] $msg")

  private def truncate(s: String, maxLen: Int = 300): String =
    val safe      = Option(s).getOrElse("")
    val flattened = safe.replaceAll("\\s+", " ").trim
    if flattened.length > maxLen then flattened.take(maxLen) + "…" else flattened

  private def textOf(content: List[ContentBlock]): String =
    content
      .collect { case ContentBlock.Text(text, _, _) => text }
      .mkString("\n")

  private def activeNamesOf(tools: List[Tool]): List[String] = tools.collect { case c: Tool.CustomRaw => c.name }

  /** Ergebnis der Ausführung eines einzelnen `ToolUse`-Blocks: das `ToolResult` (geht in die Historie zurück) sowie die dadurch neu "entdeckten" Tools (siehe `ToolCallResult.enables`).
    *
    * WICHTIG: Dieser Client kennt `search_tools` NICHT als Sonderfall - er ruft für JEDEN `ToolUse`-Block einfach `ToolCatalog.find(...).handler(...)` auf. Ob und welche Tools dabei neu
    * freigeschaltet werden, entscheidet ausschließlich der jeweilige Handler (bei den drei fachlichen Tools immer keine, bei `search_tools` die gefundenen Treffer) - siehe `ToolCatalog.scala`.
    */
  private case class ToolOutcome(result: ContentBlock.ToolResult, newlyEnabled: List[Tool.CustomRaw])

  private def executeToolUse(toolUse: ContentBlock.ToolUse): ToolOutcome =
    val callResult = ToolCatalog.find(toolUse.name) match
      case Some(tool) =>
        log(s"   >> ${toolUse.name}(${Json.fromFields(toolUse.input).noSpaces})")
        val result = tool.handler(toolUse.input)
        log(s"   << ${toolUse.name} -> ${truncate(result.output)}${if result.isError then " [isError]" else ""}")
        if result.enables.nonEmpty then log(s"      entdeckt: ${result.enables.map(_.name).mkString(", ")}")
        result
      case None       =>
        log(s"   !! Modell versucht '${toolUse.name}' aufzurufen, ist aber (noch) nicht freigeschaltet.")
        ToolCallResult.error(s"Tool '${toolUse.name}' ist nicht verfügbar - nutze zuerst search_tools.")
    ToolOutcome(ContentBlock.ToolResult(toolUseId = toolUse.id, content = callResult.output, isError = Some(callResult.isError)), callResult.enables)

  /** Führt einen Model-Call aus, ggf. als Multi-Turn Tool-Search-Loop.
    *
    * Ablauf pro Turn:
    *   1. Request wird MIT der aktuell freigeschalteten `tools`-Liste gesendet (Turn 1: nur `search_tools`, siehe `ToolCatalog.initiallyVisible`).
    *   1. Für jeden `ToolUse`-Block im Response ruft `executeToolUse` generisch `ToolCatalog.find(name).handler(...)` auf - das Ergebnis (`ToolCallResult`) enthält sowohl den Text fürs `ToolResult`
    *      als auch (nur bei `search_tools` nicht-leer) neu freigeschaltete Tools, die der `tools`-Liste des NÄCHSTEN Turns hinzugefügt werden.
    *   1. Die Schleife endet, sobald `stopReason != "tool_use"` ist.
    *
    * @param systemPrompt
    *   Rolle/Instruktion des Agenten.
    * @param userMessage
    *   die eigentliche Nutzerfrage.
    */
  def chat(model: String, systemPrompt: String, userMessage: String, maxTokens: Int = 2000): String =

    def send(messages: List[Message], activeTools: List[Tool]): MessageResponse =
      timeout(RequestTimeout) {
        client.createMessage(
          MessageRequest(model = model, messages = messages, system = Some(systemPrompt), maxTokens = maxTokens, tools = Some(activeTools)),
        )
      }

    @annotation.tailrec
    def loop(messages: List[Message], activeTools: List[Tool], turn: Int): String =
      log(s"--- Turn $turn: sende ${messages.size} Nachricht(en), aktive Tools=[${activeNamesOf(activeTools).mkString(", ")}] ---")

      val response =
        try send(messages, activeTools)
        catch
          case e: Throwable =>
            log(s"<- Fehler   ${truncate(e.getMessage)}")
            throw e

      log(s"<- Turn $turn Antwort: stop_reason=${response.stopReason.getOrElse("-")}")
      logResponse(response.content)

      if !response.stopReason.contains("tool_use") then
        val finalText = textOf(response.content)
        log(s"===== Finale Antwort nach $turn Turn(s): ${truncate(finalText)} =====")
        finalText
      else
        // ACHTUNG (sttp-ai 0.11.0): `ContentBlock.Thinking` hat kein `signature`-Feld - leere Thinking-Blöcke
        // werden defensiv herausgefiltert, bevor die Antwort in die Historie übernommen wird (siehe README des
        // Schwesterprojekts ai-sttpai-manual für Details).
        val historyContent = response.content.filterNot {
          case ContentBlock.Thinking(thinking) => thinking.isBlank
          case _                               => false
        }

        val outcomes        = response.content.collect { case tu: ContentBlock.ToolUse => tu }.map(executeToolUse)
        val activeNames     = activeNamesOf(activeTools).toSet
        // Mehrere search_tools-Aufrufe im selben Turn (z. B. parallele Anfragen des Modells) können dieselben
        // Treffer liefern - hier zusätzlich gegen bereits gesehene Namen dedupliziert (`distinctBy`, stabile
        // Reihenfolge), da die API keine doppelten Tool-Namen in der `tools`-Liste akzeptiert.
        val genuinelyNew    = outcomes.flatMap(_.newlyEnabled).filterNot(t => activeNames.contains(t.name)).distinctBy(_.name)
        val nextActiveTools = activeTools ++ genuinelyNew

        if genuinelyNew.nonEmpty then
          log(s"## Tool-Liste erweitert: [${activeNames.mkString(", ")}] -> [${activeNamesOf(nextActiveTools).mkString(", ")}]")

        // Alle tool_result-Blöcke gehören in EINEN user-Turn (Anthropic-Konvention).
        val nextMessages = messages :+ Message.assistant(historyContent) :+ Message.user(outcomes.map(_.result))
        loop(nextMessages, nextActiveTools, turn + 1)
    end loop

    log(s"===== Model-Call gestartet (model=$model) - Startwerkzeug: nur '${ToolCatalog.initiallyVisible.name}' =====")
    log(s"   system:  ${truncate(systemPrompt)}")
    log(s"   user:    ${truncate(userMessage)}")
    loop(List(Message.user(userMessage)), activeTools = List(ToolCatalog.initiallyVisible), turn = 1)

  def close(): Unit = client.close()

  private def logResponse(content: List[ContentBlock]): Unit =
    content.foreach {
      case ContentBlock.Text(text, _, _)   => log(s"   [text]      ${truncate(text)}")
      case ContentBlock.Thinking(thinking) => log(s"   [thinking]  ${truncate(thinking)}")
      case tu: ContentBlock.ToolUse        => log(s"   [tool_use]  name=${tu.name} id=${tu.id} input=${Json.fromFields(tu.input).noSpaces}")
      case other                           => log(s"   [$other]")
    }
