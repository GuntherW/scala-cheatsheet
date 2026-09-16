package agents

import io.circe.{Decoder, Json}
import io.circe.parser.decode
import sttp.ai.claude.ClaudeClient
import sttp.ai.claude.ClaudeExceptions.ClaudeException.DeserializationClaudeException
import sttp.ai.claude.config.ClaudeConfig
import sttp.ai.claude.models.{ContentBlock, Message, OutputFormat, Tool}
import sttp.ai.claude.requests.MessageRequest
import sttp.ai.claude.responses.MessageResponse
import sttp.ai.core.http.RetryingBackend
import sttp.client4.{DefaultSyncBackend, SyncBackend}
import sttp.model.Uri
import sttp.tapir.Schema

/** Dünner Wrapper um den `ClaudeClient` aus [[https://sttp-ai.softwaremill.com/ sttp-ai]] (Modul `claude`).
  *
  * sttp-ai bringt bereits einen vollwertigen, typsicheren Client für die Anthropic Messages API mit (Request-/Response-Modelle, Authentifizierung über `x-api-key`/`anthropic-version`,
  * Fehlerhierarchie `ClaudeException`) - die frühere, selbstgeschriebene Kombination aus `sttp-client4` + `upickle`-JSON-Modellen (`AnthropicModels`) entfällt dadurch komplett. Dieses Objekt bleibt
  * nur als schlanke Fassade bestehen, um das simple Logging (`[LLM:<Aufrufer>] ...`) sowie den Tool-Use-Loop zentral zu halten.
  *
  * '''Client/Backend getrennt statt `ClaudeSyncClient`''': `ClaudeClient` baut nur noch die Requests (stateless), das eigentliche Senden übernimmt ein separat gehaltener `SyncBackend`. Das ist
  * Voraussetzung dafür, künftig sttp-ai-Interceptoren (`sttp.ai.core.agent.AgentInterceptor`, siehe `Interceptors.scala`) einzusetzen - deren Agent-Loop (`Agent.run(in)(backend)`) erwartet den
  * Backend als expliziten Parameter statt ihn (wie `ClaudeSyncClient`) intern zu verstecken. Nebeneffekt: `RetryingBackend` sorgt jetzt tatsächlich für Retries bei transienten Fehlern (5xx, 429, ...) -
  * vorher war `ClaudeConfig.maxRetries` zwar konfigurierbar, wurde aber nie tatsächlich angewendet.
  */
object AnthropicClient:

  private val AnthropicVersion = "2023-06-01"

  private val apiKey: String = Env.get("ANTHROPIC_AUTH_TOKEN")
    .orElse(Env.get("ANTHROPIC_API_KEY"))
    .getOrElse(throw new RuntimeException("Weder ANTHROPIC_AUTH_TOKEN noch ANTHROPIC_API_KEY gesetzt."))

  // Dieses Projekt spricht (wie im Python-Pendant) einen requesty.ai-Router statt der offiziellen
  // Anthropic-Basis-URL an. sttp-ai unterstützt das direkt über `ClaudeConfig.baseUrl` (Standard wäre
  // `https://api.anthropic.com`) - `v1/messages` wird vom Client selbst angehängt.
  private val config = ClaudeConfig(
    apiKey = apiKey,
    anthropicVersion = AnthropicVersion,
    baseUrl = Uri.unsafeParse("https://router.eu.requesty.ai"),
  )

  private val client: ClaudeClient = ClaudeClient(config)

  private val httpBackend: SyncBackend = RetryingBackend(DefaultSyncBackend(), config.maxRetries)

  /** Schreibt eine Log-Zeile für die Kommunikation mit dem Modell auf die Konsole.
    *
    * `caller` identifiziert den Aufrufer (Agenten-Name bzw. `"Planner"`), damit sich bei parallel laufenden Agenten (siehe `Orchestrator`) erkennen lässt, welche Log-Zeile zu welchem Model-Call
    * gehört - ohne dieses Präfix wären die interleavten Konsolen-Ausgaben mehrerer gleichzeitiger LLM-Aufrufe nicht mehr auseinanderzuhalten. `indent = true` markiert Detail-Zeilen (Text-/Tool-
    * Blöcke innerhalb einer Antwort), die unter der jeweiligen "Anfrage"/"Antwort"-Zeile eingerückt dargestellt werden.
    */
  private def log(caller: String, msg: String, indent: Boolean = false): Unit =
    val prefix = if indent then "    " else ""
    println(s"[LLM:$caller] $prefix$msg")

  /** Kürzt lange, mehrzeilige Texte für die Log-Ausgabe auf eine einzelne, überschaubare Zeile (Zeilenumbrüche/mehrfache Leerzeichen werden zu einem Leerzeichen zusammengefasst).
    */
  private def truncate(s: String, maxLen: Int = 300): String =
    val safe      = Option(s).getOrElse("")
    val flattened = safe.replaceAll("\\s+", " ").trim
    if flattened.length > maxLen then flattened.take(maxLen) + "…" else flattened

  private def textOf(content: List[ContentBlock]): String =
    content
      .collect { case ContentBlock.Text(text, _, _) => text }
      .mkString("\n")

  /** Sendet einen `MessageRequest` über den getrennt gehaltenen `httpBackend` (siehe Kommentar am Objekt-Kopf) - Ersatz für das bisherige `ClaudeSyncClient.createMessage`, das Request-Bau (`client`)
    * und Versand (`httpBackend`) intern zusammenfasste.
    */
  private def sendMessage(request: MessageRequest): MessageResponse =
    client.createMessage(request).send(httpBackend).body match
      case Left(exception) => throw exception
      case Right(response) => response

  /** Ersatz für das bisherige `ClaudeSyncClient.createMessageAs[T]`: erzwingt Structured Output (falls noch nicht gesetzt) und parst die Text-Antwort zu `T`.
    */
  private def sendMessageAs[T: {Schema, Decoder}](request: MessageRequest): T =
    val withSchema =
      if request.usesStructuredOutput then request
      else request.withStructuredOutput(OutputFormat.JsonSchema.withTapirSchema[T])
    val response   = sendMessage(withSchema)
    val text       = response.content.collect { case ContentBlock.Text(t, _, _) => t }.mkString
    decode[T](text) match
      case Right(value) => value
      case Left(e)      => throw new DeserializationClaudeException(s"Failed to parse structured output: ${e.getMessage}", null)

  /** Führt einen Model-Call aus - bei Bedarf als Multi-Turn Tool-Use-Loop.
    *
    * Deckt drei Fälle in EINER Methode ab (siehe README, Abschnitt "Client-seitiges Tool"):
    *   1. '''Kein Tool''' (`useWebSearch = false`, `clientTools = Nil`): ein einzelner Request genügt, die Schleife unten beendet sich bereits nach Turn 1, da `stopReason != "tool_use"`.
    *   1. '''Nur server-seitiges Tool''' (`useWebSearch = true`): der Server löst `web_search` komplett selbst auf, meist genügt auch hier ein Request.
    *   1. '''Client-seitige (custom) Tools''' (`clientTools`, z. B. `calculate_tco`): das Modell liefert nur den Aufrufwunsch zurück (`stopReason == "tool_use"`, `ContentBlock.ToolUse`), WIR führen
    *      `toolHandlers` lokal aus und senden das Ergebnis zurück (Multi-Turn).
    *
    * '''Mix aus server- und client-seitigen Tools ist bewusst NICHT unterstützt''' (siehe `require` unten): Getestet gegen den hier genutzten Router-Endpunkt verhält sich `web_search` in dieser
    * Kombination NICHT wie ein bereits vom Server aufgelöstes Tool (kein `ContentBlock.ServerToolUse`/`WebSearchToolResult`-Paar), sondern wie ein ganz normaler `ContentBlock.ToolUse`, den WIR
    * beantworten müssten - das können wir aber nicht, da uns keine eigene Websuch-Implementierung zur Verfügung steht. Ein Agent, der beides braucht, müsste daher entweder zwei getrennte Model-Calls
    * machen oder auf eine eigene Suchimplementierung zurückgreifen - beides außerhalb des Scopes dieses Lernprojekts.
    *
    * @param caller
    *   Bezeichner des Aufrufers (Agenten-Name) für das Logging - siehe `log`.
    * @param useWebSearch
    *   ob das server-seitige `web_search`-Tool erlaubt ist
    * @param clientTools
    *   client-seitige (custom) Tool-Definitionen, z. B. `calculate_tco`
    * @param toolHandlers
    *   Mapping von Tool-Name -> Funktion, die die rohen Eingabeparameter (`Map[String, Json]`) entgegennimmt und einen String (meist JSON) zurückgibt - nur relevant für `clientTools`.
    */
  def chat(
      caller: String,
      model: String,
      systemPrompt: String,
      userMessage: String,
      useWebSearch: Boolean = false,
      clientTools: List[Tool] = Nil,
      toolHandlers: Map[String, Map[String, Json] => String] = Map.empty,
      maxTokens: Int = 2000,
  ): String =
    require(
      !(useWebSearch && clientTools.nonEmpty),
      "Mix aus server-seitigem web_search und client-seitigen Tools wird von diesem Router-Endpunkt nicht sauber unterstützt (siehe Scaladoc/README).",
    )
    val tools     = (if useWebSearch then List(Tool.WebSearch.default) else Nil) ++ clientTools
    val toolNames = (if useWebSearch then List("web_search") else Nil) ++ clientTools.collect { case c: Tool.Custom => c.name }
    val toolsDesc = if toolNames.isEmpty then "keine" else toolNames.mkString(", ")

    @annotation.tailrec
    def loop(messages: List[Message], turn: Int): String =
      log(caller, s"Anfrage (Turn $turn) ...")

      val request = MessageRequest(
        model = model,
        messages = messages,
        system = Some(systemPrompt),
        maxTokens = maxTokens,
        tools = if tools.isEmpty then None else Some(tools),
      )

      val response =
        try sendMessage(request)
        catch
          case e: Throwable =>
            log(caller, s"Fehler: ${truncate(e.getMessage)}")
            throw e

      log(caller, s"Antwort (Turn $turn): stop_reason=${response.stopReason.getOrElse("-")}")
      response.content.foreach {
        case ContentBlock.Text(text, _, _) => log(caller, s"[Text]: ${truncate(text)}", indent = true)
        case _: ContentBlock.Thinking      => // internes Nachdenken des Modells, kein inhaltliches Ergebnis - bewusst nicht geloggt (Rauschen)
        case tu: ContentBlock.ToolUse      => log(caller, s"[Tool-Aufruf]: ${tu.name}(${Json.fromFields(tu.input).noSpaces})", indent = true)
        case other                         => log(caller, s"[$other]", indent = true)
      }

      if !response.stopReason.contains("tool_use") then
        val finalText = textOf(response.content)
        log(caller, s"Fertig nach $turn Turn(s): ${truncate(finalText)}")
        finalText
      else
        // Die komplette Assistant-Antwort (inkl. ToolUse-Blöcken) muss Teil der Historie werden, damit das Modell im nächsten Turn
        // weiß, worauf sich die tool_result-Blöcke beziehen.
        //
        // ACHTUNG (sttp-ai 0.11.0): `ContentBlock.Thinking` hat - anders als das frühere, selbstgeschriebene JSON-Modell dieses
        // Projekts - KEIN `signature`-Feld. Gelegentlich liefert das Modell/der Router einen (fast) leeren `thinking`-Block zurück;
        // sendet man den unverändert zurück, lehnt die API den Request mit "each thinking block must contain thinking" ab. Da wir die
        // Signatur ohnehin nicht erhalten können, filtern wir leere Thinking-Blöcke defensiv heraus, bevor wir die Antwort in die
        // Historie übernehmen.
        val historyContent = response.content.filterNot {
          case ContentBlock.Thinking(thinking) => thinking.isBlank
          case _                               => false
        }

        val toolResultBlocks = response.content
          .collect { case tu: ContentBlock.ToolUse => tu }
          .map { tu =>
            val handler    = toolHandlers.getOrElse(tu.name, (_: Map[String, Json]) => s"Fehler: kein Handler für Tool '${tu.name}' registriert.")
            val resultText = handler(tu.input)
            log(caller, s"Tool-Ergebnis: ${tu.name} -> ${truncate(resultText)}", indent = true)
            ContentBlock.ToolResult(toolUseId = tu.id, content = resultText)
          }

        // Alle tool_result-Blöcke gehören in EINEN user-Turn (Anthropic-Konvention), nicht in mehrere separate Nachrichten.
        val nextMessages = messages :+ Message.assistant(historyContent) :+ Message.user(toolResultBlocks)
        loop(nextMessages, turn + 1)
    end loop

    log(caller, s"Model-Call gestartet (model=$model, tools=$toolsDesc)")
    log(caller, s"Auftrag: ${truncate(userMessage)}", indent = true)
    loop(List(Message.user(userMessage)), turn = 1)

  /** Führt einen Model-Call mit erzwungenem, schemakonformem JSON-Output aus (Anthropics natives "Structured Output"-Feature, `output_config` / `json_schema` - NICHT zu verwechseln mit Tool-Use).
    * `sttp-ai` leitet das JSON-Schema automatisch aus der Case-Class `T` ab (via Tapir) und parst die Antwort direkt zu `T` (circe). Im Gegensatz zu `chat` mit Tools genügt dafür immer genau ein
    * Request (kein Multi-Turn-Loop), da das Modell gezwungen wird, ausschließlich valides JSON zu liefern - es gibt keinen `tool_use`/`tool_result`-Umweg.
    *
    * @param caller
    *   Bezeichner des Aufrufers (hier i. d. R. `"Planner"`) für das Logging - siehe `log`.
    * @tparam T
    *   Ziel-Typ der Antwort, benötigt `Schema` (Tapir, für die JSON-Schema-Ableitung) und `Decoder` (circe, für das Parsen der Antwort).
    */
  def chatStructured[T: {Schema, Decoder}](
      caller: String,
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

    log(caller, s"Model-Call gestartet (model=$model, structured output)")
    log(caller, s"Auftrag: ${truncate(userMessage)}", indent = true)

    try
      val result = sendMessageAs[T](request)
      log(caller, s"Fertig: $result")
      result
    catch
      case e: Throwable =>
        log(caller, s"Fehler: ${truncate(e.getMessage)}")
        throw e

  def close(): Unit = httpBackend.close()

/** Lädt Umgebungsvariablen aus der `.env`-Datei im aktuellen Arbeitsverzeichnis (`os.pwd`, also i. d. R. diesem Projektordner, wenn `scala-cli run .` von hier aus gestartet wird), analog zum
  * Python-Pendant (`python-dotenv`).
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
