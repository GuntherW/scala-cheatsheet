package agents

import io.circe.Codec
import sttp.ai.claude.ClaudeClient
import sttp.ai.claude.agent.ClaudeAgent
import sttp.ai.claude.config.ClaudeConfig
import sttp.ai.claude.models.{ContentBlock, Message, Tool}
import sttp.ai.claude.requests.MessageRequest
import sttp.ai.core.agent.*
import sttp.ai.core.http.RetryingBackend
import sttp.client4.{Backend, DefaultSyncBackend, SyncBackend}
import sttp.model.Uri
import sttp.monad.IdentityMonad
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

  /** Pipeline-weite Sammelstelle für Tokens/Kosten/Dauer aller über `buildAgent`/`buildStructuredAgent` erzeugten LLM-Calls (inkl. Planner) - siehe `Interceptors.UsageCollector`. Der Orchestrator
    * liest daraus am Ende einer Pipeline den Gesamt-Report.
    */
  val usageCollector: Interceptors.UsageCollector = new Interceptors.UsageCollector

  private def commonInterceptors(caller: String): Seq[AgentInterceptor[Identity]] =
    Seq(Interceptors.loggingFor(caller), new Interceptors.UsageTrackingInterceptor(caller, usageCollector), Interceptors.budgetSafetyNet)

  /** Baut einen interceptor-fähigen `sttp.ai.core.agent.Agent` - unabhängig davon, ob dahinter der generische, client-seitige Tool-Use-Loop von sttp-ai (`ClaudeAgent.synchronous`) oder der bewusst
    * simple Einzel-Request-Pfad für das server-seitige `web_search`-Tool steckt (`webSearchAgent`, siehe dort). `Agent.scala` muss diesen Unterschied dadurch nicht kennen - EIN Aufruf, EIN
    * Rückgabetyp (`Agent[Identity, String, String]`), egal welcher Tool-Typ genutzt wird.
    *
    * @param caller
    *   Bezeichner des Aufrufers (Agenten-Name) - dient sowohl dem Logging als auch der Zuordnung im `usageCollector`-Report.
    * @param tools
    *   client-seitige (custom) Tools, z. B. `CalculateTcoTool.agentTool` - nur relevant, wenn `useWebSearch = false`.
    * @param useWebSearch
    *   ob statt des Tool-Use-Loops der Einzel-Request-Pfad mit dem server-seitigen `web_search`-Tool genutzt werden soll (siehe `webSearchAgent` für die Begründung, warum das kein
    *   `AgentTool`/`ClaudeAgent` sein kann).
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
    if useWebSearch then webSearchAgent(caller, model, systemPrompt, maxTokens.getOrElse(2000))
    else
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

  /** Eigene, minimale `sttp.ai.core.agent.Agent`-Implementierung für einen einmaligen Model-Call mit dem server-seitigen `web_search`-Tool (`Tool.WebSearch`, siehe sttp-ai-Doku
    * [[https://sttp-ai.softwaremill.com/claude/tool-calling.html "Tool calling", Abschnitt "Predefined tools"]]) - bewusst NICHT über `ClaudeAgent`/`AgentBuilder`.
    *
    * '''Warum keine `.tools(...)`-Liste, sondern eine eigene `Agent`-Implementierung?''' `Tool.WebSearch.default` hat den Typ `sttp.ai.claude.models.Tool` - `AgentBuilder.tools(...)` verlangt aber
    * `Seq[sttp.ai.core.agent.AgentTool[F, _]]`, einen anderen, höherwertigen Typ (Schema + lokal auszuführende Funktion). Man kann `Tool.WebSearch.default` also nicht einfach in die Tool-Liste von
    * `buildAgent` mitgeben - das wäre ein Typfehler, kein Implementierungsdetail. Das liegt NICHT am HTTP-Protokoll (`web_search` und client-seitige Tools landen in der Messages API im selben
    * `tools`-Array, siehe sttp-ai-Doku "Both custom and predefined tools can be passed in the same tools list"), sondern daran, dass `web_search` strukturell keine `AgentTool`-Eigenschaften hat: kein
    * Schema nötig (Anthropic kennt das Tool schon) und keine lokale Ausführung (der Server löst es komplett selbst auf - wir bekommen dafür nie einen `ToolCall` zum Beantworten).
    *
    * Da `web_search` aus demselben Grund (keine lokale Ausführung) auch nie einen Multi-Turn-Loop braucht - der Server löst das Tool (inkl. eventuell mehrfacher interner Suchen) komplett innerhalb
    * EINES HTTP-Response auf -, genügt strukturell ein einzelner Request. Statt dafür trotzdem einen vollen `LoopAgent` samt eigenem `AgentBackend` zu bemühen (reiner Overhead für eine einzige
    * Iteration), implementiert diese Methode das öffentliche `sttp.ai.core.agent.Agent`-Trait direkt: eine Iteration, ein `AgentResult`, aber denselben `aroundLlmCall`-Interceptor-Hook
    * (Logging/Usage-Tracking) wie `buildAgent` - und, anders als eine schlichte `String`-Rückgabe es könnte, dieselbe "unsauberes Ende"-Erkennung (`FinishReason.TokenLimit` ->
    * `Left(AgentIncomplete(...))`), die `Agent.scala` einheitlich für BEIDE Tool-Typen behandelt.
    */
  private def webSearchAgent(caller: String, model: String, systemPrompt: String, maxTokens: Int): Agent[Identity, String, String] =
    new Agent[Identity, String, String]:
      protected given monad: sttp.monad.MonadError[Identity] = IdentityMonad

      def run(userMessage: String, seedHistory: ConversationHistory)(backend: Backend[Identity]): Identity[AgentResult[Either[AgentFailure, String]]] =
        val history = seedHistory.addUserPrompt(userMessage)
        val request = MessageRequest(
          model = model,
          messages = List(Message.user(userMessage)),
          system = Some(systemPrompt),
          maxTokens = maxTokens,
          tools = Some(List(Tool.WebSearch.default)),
        )

        val interceptor    = AgentInterceptor.compose(commonInterceptors(caller))
        val llmCallContext = LlmCallContext(history, includeTools = true, IterationInfo(iteration = 1, maxIterations = 1))

        val response = interceptor.aroundLlmCall(llmCallContext) {
          client.createMessage(request).send(backend).body match
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

        val usage                                     = response.usage.getOrElse(TokenUsage.Zero)
        val finalHistory                              = history.addAssistantResponse(response.textContent, Seq.empty)
        val finalAnswer: Either[AgentFailure, String] =
          if response.stopReason == StopReason.MaxTokens then Left(AgentIncomplete(response.textContent, FinishReason.TokenLimit, parseError = None))
          else Right(response.textContent)

        AgentResult(
          finalAnswer,
          iterations = 1,
          toolCalls = Seq.empty,
          finishReason = if response.stopReason == StopReason.MaxTokens then FinishReason.TokenLimit else FinishReason.NaturalStop,
          usage = usage,
          llmCalls = Seq(LlmCallUsage(response.model, usage)),
          history = finalHistory,
        )

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
