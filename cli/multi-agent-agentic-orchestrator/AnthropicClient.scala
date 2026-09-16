package agents

import io.circe.{Codec, Decoder}
import io.circe.parser.decode
import sttp.ai.claude.ClaudeClient
import sttp.ai.claude.ClaudeExceptions.ClaudeException.DeserializationClaudeException
import sttp.ai.claude.config.ClaudeConfig
import sttp.ai.claude.models.{ClaudeModel, ContentBlock, Message, OutputFormat}
import sttp.ai.claude.requests.MessageRequest
import sttp.ai.claude.responses.MessageResponse
import sttp.ai.core.agent.{Agent, AgentBuilder, AgentTool}
import sttp.ai.core.http.RetryingBackend
import sttp.client4.{DefaultSyncBackend, SyncBackend}
import sttp.model.Uri
import sttp.shared.Identity
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

  /** Pipeline-weite Sammelstelle für Tokens/Kosten/Dauer aller über `buildAgent`/`buildStructuredAgent` erzeugten Agenten (inkl. Planner) - siehe `Interceptors.UsageCollector`. Der Orchestrator liest
    * daraus am Ende einer Pipeline den Gesamt-Report.
    */
  val usageCollector: Interceptors.UsageCollector = new Interceptors.UsageCollector

  private def commonInterceptors(caller: String) =
    Seq(Interceptors.loggingFor(caller), new Interceptors.UsageTrackingInterceptor(caller, usageCollector), Interceptors.budgetSafetyNet)

  /** Baut einen interceptor-fähigen Agent-Loop (`sttp.ai.core.agent.Agent`) für einen Tool-Use-Agenten - der Ersatz für den bisherigen, handgeschriebenen Multi-Turn-Loop in `chat`. Nutzt das eigene
    * `ClaudeToolLoopBackend` (siehe `AgentBackends.scala`) statt der eingebauten (aber `private[claude]` und ohne `web_search`-Unterstützung auskommenden) `ClaudeAgent`-Fabrik von sttp-ai.
    *
    * @param caller
    *   Bezeichner des Aufrufers (Agenten-Name) - dient sowohl dem Logging als auch der Zuordnung im `usageCollector`-Report.
    * @param tools
    *   client-seitige (custom) Tools, z. B. `CalculateTcoTool.agentTool`.
    * @param useWebSearch
    *   ob zusätzlich das server-seitige `web_search`-Tool angeboten werden soll (siehe `ClaudeToolLoopBackend`).
    */
  def buildAgent(
      caller: String,
      model: String,
      systemPrompt: String,
      tools: Seq[AgentTool[Identity, ?]] = Seq.empty,
      useWebSearch: Boolean = false,
      maxIterations: Int = 10,
      maxTokens: Option[Int] = None,
  ): Agent[Identity, String, String] =
    val builder = AgentBuilder[Identity, ClaudeModel.CustomClaudeModel](cfg => ClaudeToolLoopBackend(client, model, useWebSearch, cfg))
      .maxIterations(maxIterations)
      .systemPrompt(systemPrompt)
      .tools(tools)
      .interceptors(commonInterceptors(caller))
    maxTokens.fold(builder)(builder.maxTokens).build

  /** Wie `buildAgent`, aber mit erzwungenem, schemakonformem JSON-Output (Structured Output) - Ersatz für `chatStructured`. Ein einzelner Request genügt normalerweise (`maxIterations = 1`), da das
    * Modell zu validem JSON gezwungen wird.
    */
  def buildStructuredAgent[T: {Schema, Codec}](
      caller: String,
      model: String,
      systemPrompt: String,
      maxIterations: Int = 1,
  ): Agent[Identity, String, T] =
    AgentBuilder[Identity, ClaudeModel.CustomClaudeModel](cfg => ClaudeToolLoopBackend(client, model, includeWebSearch = false, cfg))
      .maxIterations(maxIterations)
      .systemPrompt(systemPrompt)
      .interceptors(commonInterceptors(caller))
      .deriveResponseSchema[T]
      .build

  def backend: SyncBackend = httpBackend

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
