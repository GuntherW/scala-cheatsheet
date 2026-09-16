package agents

import io.circe.Codec
import sttp.ai.claude.ClaudeClient
import sttp.ai.claude.agent.ClaudeAgent
import sttp.ai.claude.config.ClaudeConfig
import sttp.ai.claude.models.{ContentBlock, Message, Tool}
import sttp.ai.claude.requests.MessageRequest
import sttp.ai.core.agent.*
import sttp.ai.core.http.RetryingBackend
import sttp.client4.{DefaultSyncBackend, SyncBackend}
import sttp.model.Uri
import sttp.shared.Identity
import sttp.tapir.Schema

/** Dünner Wrapper um den `ClaudeClient` aus [[https://sttp-ai.softwaremill.com/ sttp-ai]] (Modul `claude`).
  *
  * sttp-ai bringt bereits einen vollwertigen, typsicheren Client für die Anthropic Messages API mit (Request-/Response-Modelle, Authentifizierung über `x-api-key`/`anthropic-version`,
  * Fehlerhierarchie `ClaudeException`) - die frühere, selbstgeschriebene Kombination aus `sttp-client4` + `upickle`-JSON-Modellen (`AnthropicModels`) entfällt dadurch komplett. Dieses Objekt bleibt
  * als schlanke Fassade bestehen, die für alle Agenten (siehe `Agent.scala`/`AgentPlanner.scala`) fertig verdrahtete `sttp.ai.core.agent.Agent`-Instanzen inkl. Interceptor-Stack (Logging/Usage-
  * Tracking/Budget, siehe `Interceptors.scala`) bereitstellt (`buildAgent`/`buildStructuredAgent`), auf Basis der eingebauten `ClaudeAgent`-Fabrik von sttp-ai. Für das server-seitige
  * `web_search`-Tool (das sich NICHT über die `AgentTool`-Abstraktion ausdrücken lässt, siehe `runWithWebSearch`) gibt es einen bewusst simplen, eigenständigen Einzel-Request-Pfad statt eines eigenen
  * `AgentBackend`.
  *
  * '''Client/Backend getrennt statt `ClaudeSyncClient`''': `ClaudeClient` baut nur noch die Requests (stateless), das eigentliche Senden übernimmt ein separat gehaltener `SyncBackend`. Das ist
  * Voraussetzung dafür, sttp-ai-Interceptoren (`sttp.ai.core.agent.AgentInterceptor`, siehe `Interceptors.scala`) einzusetzen - deren Agent-Loop (`Agent.run(in)(backend)`) erwartet den Backend als
  * expliziten Parameter statt ihn (wie `ClaudeSyncClient`) intern zu verstecken. Nebeneffekt: `RetryingBackend` sorgt jetzt tatsächlich für Retries bei transienten Fehlern (5xx, 429, ...) - vorher
  * war `ClaudeConfig.maxRetries` zwar konfigurierbar, wurde aber nie tatsächlich angewendet.
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

  def close(): Unit = httpBackend.close()

  def backend: SyncBackend = httpBackend

  /** Pipeline-weite Sammelstelle für Tokens/Kosten/Dauer aller über `buildAgent`/`buildStructuredAgent`/`runWithWebSearch` erzeugten LLM-Calls (inkl. Planner) - siehe `Interceptors.UsageCollector`.
    * Der Orchestrator liest daraus am Ende einer Pipeline den Gesamt-Report.
    */
  val usageCollector: Interceptors.UsageCollector = new Interceptors.UsageCollector

  private def commonInterceptors(caller: String): Seq[AgentInterceptor[Identity]] =
    Seq(Interceptors.loggingFor(caller), new Interceptors.UsageTrackingInterceptor(caller, usageCollector), Interceptors.budgetSafetyNet)

  /** Baut einen interceptor-fähigen Agent-Loop (`sttp.ai.core.agent.Agent`) für einen client-seitigen Tool-Use-Agenten, auf Basis der eingebauten `ClaudeAgent`-Fabrik von sttp-ai
    * (`sttp.ai.claude.agent.ClaudeAgent.synchronous`). Passend für alle Agenten, deren Tools sich als `AgentTool` ausdrücken lassen (client-seitig, vom Modell angefragt, von UNS lokal ausgeführt) -
    * für das server-seitige `web_search`-Tool siehe stattdessen `runWithWebSearch`.
    *
    * @param caller
    *   Bezeichner des Aufrufers (Agenten-Name) - dient sowohl dem Logging als auch der Zuordnung im `usageCollector`-Report.
    * @param tools
    *   client-seitige (custom) Tools, z. B. `CalculateTcoTool.agentTool`.
    */
  def buildAgent(
      caller: String,
      model: String,
      systemPrompt: String,
      tools: Seq[AgentTool[Identity, ?]] = Seq.empty,
      maxIterations: Int = 10,
      maxTokens: Option[Int] = None,
  ): Agent[Identity, String, String] =
    val builder = ClaudeAgent
      .synchronous(client, model)
      .maxIterations(maxIterations)
      .systemPrompt(systemPrompt)
      .tools(tools)
      .interceptors(commonInterceptors(caller))
    maxTokens.fold(builder)(builder.maxTokens).build

  /** Wie `buildAgent`, aber mit erzwungenem, schemakonformem JSON-Output (Structured Output) - Ersatz für das frühere `chatStructured`. Ein einzelner Request genügt normalerweise (`maxIterations =
    * 1`), da das Modell zu validem JSON gezwungen wird.
    */
  def buildStructuredAgent[T: {Schema, Codec}](
      caller: String,
      model: String,
      systemPrompt: String,
      maxIterations: Int = 1,
  ): Agent[Identity, String, T] =
    ClaudeAgent
      .synchronous(client, model)
      .maxIterations(maxIterations)
      .systemPrompt(systemPrompt)
      .interceptors(commonInterceptors(caller))
      .deriveResponseSchema[T]
      .build

  /** Einmaliger Model-Call mit dem server-seitigen `web_search`-Tool (`Tool.WebSearch`, siehe sttp-ai-Doku
    * [[https://sttp-ai.softwaremill.com/claude/tool-calling.html "Tool calling", Abschnitt "Predefined tools"]]) - bewusst NICHT über `buildAgent`/`ClaudeAgent`.
    *
    * '''Warum kein `AgentBackend`/Agent-Loop dafür?''' `web_search` lässt sich strukturell nicht als `AgentTool` ausdrücken: `AgentTool[F, T]` verlangt zwingend ein JSON-Schema UND eine lokal
    * auszuführende Funktion (`execute: T => F[String]`) - `web_search` hat keins von beidem, da Anthropic das Tool komplett serverseitig auflöst (wir bekommen dafür nie einen `ToolCall` zum
    * Beantworten). Das ist keine Beschränkung des HTTP-Protokolls (`web_search` und client-seitige Tools landen in der Messages API im selben `tools`-Array, siehe `sttp-ai`-Doku "Both custom and
    * predefined tools can be passed in the same tools list"), sondern der `AgentTool`-Abstraktion von sttp-ai - `AgentBuilder`/`AgentConfig` bieten keinen Konfigurations-Hook für zusätzliche,
    * provider-native `Tool`-Werte neben den `AgentTool`s.
    *
    * Zusätzlich braucht `web_search` - anders als client-seitige Tools - ohnehin keinen Multi-Turn-Loop: Der Server löst das Tool (inkl. eventuell mehrfacher interner Suchen) komplett innerhalb EINES
    * HTTP-Response auf, wir bekommen nur das fertige Ergebnis zurück. Ein einzelner Request genügt daher strukturell, ein `LoopAgent` wäre hier reiner Overhead.
    *
    * Damit dieser Einzel-Request trotzdem einheitlich geloggt und im `usageCollector` erfasst wird, wird der `aroundLlmCall`-Interceptor- Hook (derselbe wie in `buildAgent`) einmalig direkt
    * aufgerufen - ohne den kompletten `LoopAgent`/`AgentBackend`-Apparat zu benötigen.
    */
  def runWithWebSearch(caller: String, model: String, systemPrompt: String, userMessage: String, maxTokens: Int = 2000): String =
    val request = MessageRequest(
      model = model,
      messages = List(Message.user(userMessage)),
      system = Some(systemPrompt),
      maxTokens = maxTokens,
      tools = Some(List(Tool.WebSearch.default)),
    )

    val interceptor    = AgentInterceptor.compose(commonInterceptors(caller))
    val history        = ConversationHistory.empty.addUserPrompt(userMessage)
    val iterationInfo  = IterationInfo(iteration = 1, maxIterations = 1)
    val llmCallContext = LlmCallContext(history, includeTools = true, iterationInfo)

    val response = interceptor.aroundLlmCall(llmCallContext) {
      client.createMessage(request).send(httpBackend).body match
        case Left(error) => throw new RuntimeException(s"Claude API error: ${error.getMessage}")
        case Right(resp) =>
          val textContent = resp.content.collectFirst { case ContentBlock.Text(text, _, _) => text }.getOrElse("")
          val u           = resp.usage
          val usage       = TokenUsage(
            inputTokens = Tokens(u.totalInputTokens.toLong),
            outputTokens = Tokens(u.outputTokens.toLong),
            cachedInputTokens = Tokens(u.cacheReadInputTokens.getOrElse(0).toLong),
            reasoningTokens = Tokens.Zero,
            cacheWriteInputTokens = Tokens(u.cacheCreationInputTokens.getOrElse(0).toLong),
          )
          val stopReason  = if resp.stopReason.contains("max_tokens") then StopReason.MaxTokens else StopReason.EndTurn
          AgentResponse(textContent, Seq.empty, stopReason, usage = Some(usage), model = Some(resp.model))
    }
    response.textContent

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
