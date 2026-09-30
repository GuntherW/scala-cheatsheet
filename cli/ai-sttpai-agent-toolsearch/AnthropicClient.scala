package agent

import io.circe.Json
import sttp.ai.claude.ClaudeSyncClient
import sttp.ai.claude.config.ClaudeConfig
import sttp.ai.claude.models.{ContentBlock, Message, Tool}
import sttp.ai.claude.requests.MessageRequest
import sttp.model.Uri
import ox.timeout

import scala.concurrent.duration.*

/** Dünner Wrapper um den `ClaudeSyncClient` aus [[https://sttp-ai.softwaremill.com/ sttp-ai]] (Modul `claude`), analog zu `ai-sttpai-manual/AnthropicClient.scala` - mit einer zentralen Ergänzung: Der
  * Tool-Use-Loop hält hier nicht eine FESTE `tools`-Liste über alle Turns hinweg, sondern eine, die WÄCHST, sobald das Modell über `search_tools` neue Werkzeuge "entdeckt" (Tool-Search-/
  * Progressive-Tool-Disclosure-Paradigma, siehe README).
  */
object AnthropicClient:

  private val AnthropicVersion = "2023-06-01"

  private val apiKey: String = Env.get("ANTHROPIC_AUTH_TOKEN")
    .orElse(Env.get("ANTHROPIC_API_KEY"))
    .getOrElse(throw new RuntimeException("Weder ANTHROPIC_AUTH_TOKEN noch ANTHROPIC_API_KEY gesetzt."))

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

  /** Führt einen Model-Call aus, ggf. als Multi-Turn Tool-Search-Loop.
    *
    * Ablauf pro Turn:
    *   1. Request wird MIT der aktuell freigeschalteten `tools`-Liste gesendet (Turn 1: nur `search_tools`).
    *   1. Ruft das Modell `search_tools` auf: Handler durchsucht die `ToolRegistry`, das Ergebnis (Treffer-Namen + Descriptions) geht als `ToolResult` zurück, UND die gefundenen Tool-Definitionen
    *      werden der `tools`-Liste für den NÄCHSTEN Turn hinzugefügt ("Freischaltung").
    *   1. Ruft das Modell eines der freigeschalteten "echten" Tools auf (z. B. `roll_dice`): der zugehörige Handler wird ausgeführt, das Ergebnis geht als `ToolResult` zurück - ganz normaler
    *      Tool-Use-Loop wie bei einem client-seitigen Tool.
    *   1. Die Schleife endet, sobald `stopReason != "tool_use"` ist.
    *
    * @param systemPrompt
    *   Rolle/Instruktion des Agenten.
    * @param userMessage
    *   die eigentliche Nutzerfrage.
    */
  def chat(model: String, systemPrompt: String, userMessage: String, maxTokens: Int = 2000): String =

    def toolResultFor(toolUse: ContentBlock.ToolUse): (ContentBlock.ToolResult, List[Tool.Custom]) =
      if toolUse.name == SearchToolsTool.Name then
        log(s"   >> search_tools(query=${toolUse.input.get("query").flatMap(_.asString).getOrElse("")})")
        val (resultJson, hitNames) = SearchToolsTool.search(toolUse.input)
        if hitNames.isEmpty then log("   << Treffer: keine")
        else log(s"   << Treffer: ${hitNames.mkString(", ")}")
        val newlyEnabled           = hitNames.flatMap(ToolRegistry.find).map(_.definition)
        (ContentBlock.ToolResult(toolUseId = toolUse.id, content = resultJson), newlyEnabled)
      else
        val meta       = ToolRegistry.find(toolUse.name)
        val resultText = meta match
          case Some(m) =>
            log(s"   >> Tool-Aufruf ${toolUse.name}(${Json.fromFields(toolUse.input).noSpaces})")
            val r = m.handler(toolUse.input)
            log(s"   << Tool-Ergebnis ${toolUse.name} -> ${truncate(r)}")
            r
          case None    =>
            log(s"   !! Modell versucht '${toolUse.name}' aufzurufen, ist aber (noch) nicht freigeschaltet.")
            Json.obj("error" -> Json.fromString(s"Tool '${toolUse.name}' ist nicht verfügbar - nutze zuerst search_tools.")).noSpaces
        (ContentBlock.ToolResult(toolUseId = toolUse.id, content = resultText), Nil)

    @annotation.tailrec
    def loop(messages: List[Message], activeTools: List[Tool], turn: Int): String =
      log(s"--- Turn $turn: sende ${messages.size} Nachricht(en), aktive Tools=[${activeTools.collect { case c: Tool.Custom => c.name }.mkString(", ")}] ---")

      val request = MessageRequest(
        model = model,
        messages = messages,
        system = Some(systemPrompt),
        maxTokens = maxTokens,
        tools = Some(activeTools),
      )

      val response =
        try timeout(RequestTimeout)(client.createMessage(request))
        catch
          case e: Throwable =>
            log(s"<- Fehler   ${truncate(e.getMessage)}")
            throw e

      log(s"<- Turn $turn Antwort: stop_reason=${response.stopReason.getOrElse("-")}")
      response.content.foreach {
        case ContentBlock.Text(text, _, _)   => log(s"   [text]      ${truncate(text)}")
        case ContentBlock.Thinking(thinking) => log(s"   [thinking]  ${truncate(thinking)}")
        case tu: ContentBlock.ToolUse        => log(s"   [tool_use]  name=${tu.name} id=${tu.id} input=${Json.fromFields(tu.input).noSpaces}")
        case other                           => log(s"   [$other]")
      }

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

        val toolUses                 = response.content.collect { case tu: ContentBlock.ToolUse => tu }
        val resultsAndNewTools       = toolUses.map(toolResultFor)
        val toolResultBlocks         = resultsAndNewTools.map(_._1)
        val newlyEnabledFromThisTurn = resultsAndNewTools.flatMap(_._2)
        val activeNames              = activeTools.collect { case c: Tool.Custom => c.name }.toSet
        // Mehrere search_tools-Aufrufe im selben Turn (z. B. parallele Anfragen des Modells) können dieselben
        // Treffer liefern - hier zusätzlich gegen bereits gesehene Namen innerhalb dieses Turns dedupliziert,
        // die API akzeptiert keine doppelten Tool-Namen in der `tools`-Liste.
        val genuinelyNewTools        = newlyEnabledFromThisTurn
          .filterNot(t => activeNames.contains(t.name))
          .groupBy(_.name)
          .values
          .map(_.head)
          .toList
        val nextActiveTools          = activeTools ++ genuinelyNewTools

        if genuinelyNewTools.nonEmpty then
          log(s"## Tool-Liste erweitert: [${activeNames.mkString(", ")}] -> [${nextActiveTools.collect { case c: Tool.Custom => c.name }.mkString(", ")}]")

        // Alle tool_result-Blöcke gehören in EINEN user-Turn (Anthropic-Konvention).
        val nextMessages = messages :+ Message.assistant(historyContent) :+ Message.user(toolResultBlocks)
        loop(nextMessages, nextActiveTools, turn + 1)
    end loop

    log(s"===== Model-Call gestartet (model=$model) - Startwerkzeug: nur '${SearchToolsTool.Name}' =====")
    log(s"   system:  ${truncate(systemPrompt)}")
    log(s"   user:    ${truncate(userMessage)}")
    loop(List(Message.user(userMessage)), activeTools = List(SearchToolsTool.definition), turn = 1)

  def close(): Unit = client.close()
