package agents

import io.circe.{Decoder, Json}
import sttp.ai.claude.ClaudeSyncClient
import sttp.ai.claude.config.ClaudeConfig
import sttp.ai.claude.models.{ContentBlock, Message, Tool}
import sttp.ai.claude.requests.MessageRequest
import sttp.model.Uri
import sttp.tapir.Schema

/** Dünner Wrapper um den `ClaudeSyncClient` aus [[https://sttp-ai.softwaremill.com/ sttp-ai]] (Modul `claude`).
  *
  * sttp-ai bringt bereits einen vollwertigen, typsicheren Client für die Anthropic Messages API mit (Request-/Response-Modelle, Authentifizierung über `x-api-key`/`anthropic-version`,
  * Fehlerhierarchie `ClaudeException`) - die frühere, selbstgeschriebene Kombination aus `sttp-client4` + `upickle`-JSON-Modellen (`AnthropicModels`) entfällt dadurch komplett. Dieses Objekt bleibt
  * nur als schlanke Fassade bestehen, um das simple Logging (`[Anthropic] ...`) sowie den Tool-Use-Loop zentral zu halten.
  */
object AnthropicClient:

  private val AnthropicVersion = "2023-06-01"

  private val apiKey: String =
    Env.get("ANTHROPIC_AUTH_TOKEN").orElse(Env.get("ANTHROPIC_API_KEY"))
      .getOrElse(throw new RuntimeException("Weder ANTHROPIC_AUTH_TOKEN noch ANTHROPIC_API_KEY gesetzt."))

  // Dieses Projekt spricht (wie im Python-Pendant) einen requesty.ai-Router statt der offiziellen
  // Anthropic-Basis-URL an. sttp-ai unterstützt das direkt über `ClaudeConfig.baseUrl` (Standard wäre
  // `https://api.anthropic.com`) - `v1/messages` wird vom Client selbst angehängt.
  private val config = ClaudeConfig(
    apiKey = apiKey,
    anthropicVersion = AnthropicVersion,
    baseUrl = Uri.unsafeParse("https://router.eu.requesty.ai"),
  )

  private val client = ClaudeSyncClient(config)

  /** Schreibt eine Log-Zeile für die Ein-/Ausgabe-Kommunikation mit dem Modell auf die Konsole. Zentraler Ort für dieses simple `println`-Logging, damit sich Aufbau und Präfix (`[Anthropic]`) nicht
    * in jeder Methode wiederholen.
    */
  private def log(msg: String): Unit = println(s"[Anthropic] $msg")

  /** Kürzt lange, mehrzeilige Texte für die Log-Ausgabe auf eine einzelne, überschaubare Zeile (Zeilenumbrüche/mehrfache Leerzeichen werden zu einem Leerzeichen zusammengefasst).
    */
  private def truncate(s: String, maxLen: Int = 300): String =
    val safe      = Option(s).getOrElse("")
    val flattened = safe.replaceAll("\\s+", " ").trim
    if flattened.length > maxLen then flattened.take(maxLen) + "…" else flattened

  private def textOf(content: List[ContentBlock]): String =
    content.collect { case ContentBlock.Text(text, _, _) => text }.mkString("\n")

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
      maxTokens: Int = 3000,
  ): String =
    val request = MessageRequest(
      model = model,
      messages = List(Message.user(userMessage)),
      system = Some(systemPrompt),
      maxTokens = maxTokens,
      tools = if useWebSearch then Some(List(Tool.WebSearch.default)) else None,
    )

    log(s"-> Request  model=$model web_search=$useWebSearch")
    log(s"   system:  ${truncate(systemPrompt)}")
    log(s"   user:    ${truncate(userMessage)}")

    try
      val response = client.createMessage(request)
      log(s"<- Response stop_reason=${response.stopReason.getOrElse("-")}")
      val text     = textOf(response.content)
      log(s"   text:    ${truncate(text)}")
      text
    catch
      case e: Throwable =>
        log(s"<- Fehler   ${truncate(e.getMessage)}")
        throw e

  /** Führt einen Tool-Use-Loop mit einem oder mehreren client-seitigen (custom) Tools aus.
    *
    * Ablauf (siehe auch README, Abschnitt "Client-seitiges Tool"):
    *   1. Request mit der Nutzeranfrage + Tool-Definition(en) senden.
    *   2. Antwortet das Modell mit `stopReason == "tool_use"`, enthält die Antwort einen oder mehrere `ContentBlock.ToolUse`-Blöcke.
    *   3. Für jeden Block wird der passende Handler aus `toolHandlers` aufgerufen (bekommt die rohen Eingabeparameter als `Map[String, Json]`).
    *   4. Die komplette Assistant-Antwort (inkl. Tool-Use-Blöcken) sowie EIN `user`-Turn mit allen `ContentBlock.ToolResult`s werden als neue Nachrichten angehängt, danach wird erneut gesendet.
    *   5. Wiederholen, bis `stopReason != "tool_use"` ist.
    *
    * @param toolHandlers
    *   Mapping von Tool-Name -> Funktion, die die rohen Eingabeparameter (`Map[String, Json]`) entgegennimmt und einen String (meist JSON) zurückgibt.
    */
  def chatWithTool(
      model: String,
      systemPrompt: String,
      userMessage: String,
      tools: List[Tool],
      toolHandlers: Map[String, Map[String, Json] => String],
      maxTokens: Int = 2000,
  ): String =

    @annotation.tailrec
    def loop(messages: List[Message], turn: Int): String =
      log(s"--- Turn $turn: sende ${messages.size} Nachricht(en) an das Modell ---")

      val request = MessageRequest(
        model = model,
        messages = messages,
        system = Some(systemPrompt),
        maxTokens = maxTokens,
        tools = Some(tools),
      )

      val response =
        try client.createMessage(request)
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
        log(s"===== Tool-Use-Loop beendet nach $turn Turn(s), finale Antwort: ${truncate(finalText)} =====")
        finalText
      else
        // Die komplette Assistant-Antwort (inkl. ToolUse-Blöcken) muss Teil der
        // Historie werden, damit das Modell im nächsten Turn weiß, worauf sich
        // die tool_result-Blöcke beziehen.
        //
        // ACHTUNG (sttp-ai 0.11.0): `ContentBlock.Thinking` hat - anders als das
        // frühere, selbstgeschriebene JSON-Modell dieses Projekts - KEIN
        // `signature`-Feld. Gelegentlich liefert das Modell/der Router einen
        // (fast) leeren `thinking`-Block zurück; sendet man den unverändert
        // zurück, lehnt die API den Request mit "each thinking block must
        // contain thinking" ab. Da wir die Signatur ohnehin nicht erhalten
        // können, filtern wir leere Thinking-Blöcke defensiv heraus, bevor wir
        // die Antwort in die Historie übernehmen.
        val historyContent = response.content.filterNot {
          case ContentBlock.Thinking(thinking) => thinking.isBlank
          case _                               => false
        }

        val toolResultBlocks = response.content
          .collect { case tu: ContentBlock.ToolUse => tu }
          .map { tu =>
            log(s"   >> Tool-Aufruf     ${tu.name}(${Json.fromFields(tu.input).noSpaces})")
            val handler    = toolHandlers.getOrElse(tu.name, (_: Map[String, Json]) => s"Fehler: kein Handler für Tool '${tu.name}' registriert.")
            val resultText = handler(tu.input)
            log(s"   << Tool-Ergebnis   ${tu.name} -> ${truncate(resultText)}")
            ContentBlock.ToolResult(toolUseId = tu.id, content = resultText)
          }

        // Alle tool_result-Blöcke gehören in EINEN user-Turn (Anthropic-Konvention),
        // nicht in mehrere separate Nachrichten.
        val nextMessages = messages :+ Message.assistant(historyContent) :+ Message.user(toolResultBlocks)
        loop(nextMessages, turn + 1)
    end loop

    log(s"===== Tool-Use-Loop gestartet (model=$model, tools=${tools.collect { case c: Tool.Custom => c.name }.mkString(", ")}) =====")
    log(s"   user:    ${truncate(userMessage)}")
    loop(List(Message.user(userMessage)), turn = 1)

  /** Führt einen Model-Call mit erzwungenem, schemakonformem JSON-Output aus (Anthropics natives "Structured Output"-Feature, `output_config` / `json_schema` - NICHT zu verwechseln mit Tool-Use).
    * `sttp-ai` leitet das JSON-Schema automatisch aus der Case-Class `T` ab (via Tapir) und parst die Antwort direkt zu `T` (circe). Im Gegensatz zu `chatWithTool` genügt dafür immer genau ein
    * Request (kein Multi-Turn-Loop), da das Modell gezwungen wird, ausschließlich valides JSON zu liefern - es gibt keinen `tool_use`/`tool_result`-Umweg.
    *
    * @tparam T
    *   Ziel-Typ der Antwort, benötigt `Schema` (Tapir, für die JSON-Schema-Ableitung) und `Decoder` (circe, für das Parsen der Antwort).
    */
  def chatStructured[T: Schema: Decoder](
      model: String,
      systemPrompt: String,
      userMessage: String,
      maxTokens: Int = 1000,
  ): T =
    val request = MessageRequest.withSystem(
      model = model,
      system = systemPrompt,
      messages = List(Message.user(userMessage)),
      maxTokens = maxTokens,
    )

    log(s"-> Request (structured output) model=$model")
    log(s"   system:  ${truncate(systemPrompt)}")
    log(s"   user:    ${truncate(userMessage)}")

    try
      val result = client.createMessageAs[T](request)
      log(s"<- Response (structured output): $result")
      result
    catch
      case e: Throwable =>
        log(s"<- Fehler   ${truncate(e.getMessage)}")
        throw e

  def close(): Unit = client.close()

/** Lädt Umgebungsvariablen aus der `.env`-Datei im Projekt-Root (eine Ebene über diesem Ordner), analog zum Python-Pendant (`python-dotenv`).
  *
  * Eigene, bewusst simple Implementierung auf Basis von `os-lib` statt der Java-Bibliothek `dotenv-java`: Letztere ist reines JVM-Java und würde als einzige Abhängigkeit dieses Projekts die
  * Scala-Native-Kompatibilität der übrigen Abhängigkeiten (`sttp-ai`, `os-lib`, `ox`) brechen. Unterstütztes Format: `SCHLÜSSEL=WERT` pro Zeile, `#`-Kommentare und Leerzeilen werden ignoriert, ein-
  * oder doppelte Anführungszeichen um den Wert werden entfernt. Fällt automatisch auf echte Umgebungsvariablen zurück, falls die `.env`-Datei fehlt oder der Schlüssel dort nicht gesetzt ist.
  */
object Env:

  private val dotenvPath = os.pwd / ".env"

  private val dotenvVars: Map[String, String] =
    if os.exists(dotenvPath) then
      os.read
        .lines(dotenvPath)
        .iterator
        .map(_.trim)
        .filter(line => line.nonEmpty && !line.startsWith("#"))
        .flatMap(_.split("=", 2) match
          case Array(key, value) => Some(key.trim -> unquote(value.trim))
          case _                 => None,
        )
        .toMap
    else Map.empty

  private def unquote(value: String): String =
    val isQuoted = value.length >= 2 && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))
    if isQuoted then value.substring(1, value.length - 1) else value

  def get(key: String): Option[String] = dotenvVars.get(key).orElse(sys.env.get(key))

  def require(key: String): String = get(key)
    .getOrElse(throw new RuntimeException(s"Umgebungsvariable '$key' ist weder in .env noch im Environment gesetzt."))
