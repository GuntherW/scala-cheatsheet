package agents

import io.circe.Json
import sttp.ai.claude.ClaudeClient
import sttp.ai.claude.models.{ContentBlock, Message, OutputConfig, OutputFormat, Tool}
import sttp.ai.claude.requests.MessageRequest
import sttp.ai.core.agent.*
import sttp.client4.Backend
import sttp.shared.Identity

/** Eigene, projekt-lokale Implementierung von `sttp.ai.core.agent.AgentBackend` - dem Adapter, über den der generische Agent-Loop (`sttp.ai.core.agent.LoopAgent`, erzeugt via
  * `AgentBuilder`/`ClaudeAgent`) mit einer konkreten LLM-API spricht.
  *
  * '''Warum nicht einfach `ClaudeAgent.synchronous(...)` (die eingebaute sttp-ai-Fabrik für Claude-Agenten) nutzen?''' NICHT weil server- und client-seitige Tools sich auf HTTP-/JSON-Ebene
  * grundsätzlich unterscheiden würden - in der Anthropic Messages API landen beide schlicht als Einträge im selben `tools`-Array, und genau das machen wir unten auch (`convertedTools` mischt
  * `Tool.WebSearch.default` mit den zu `Tool.CustomRaw` konvertierten `AgentTool`s). Der eigentliche Grund liegt konkret im sttp-ai-Code:
  *
  *   1. `sttp.ai.claude.models.Tool` ist ein Sum-Type mit unterschiedlichen Shapes: `Tool.WebSearch` hat gar kein `inputSchema`-Feld (Anthropic kennt das Tool schon), sondern eigene Felder
  *      (`maxUses`, `allowedDomains`, ...) und einen eigenen Wire-Typ (`"web_search_20250305"`) - strukturell etwas anderes als `Tool.Custom`/`Tool.CustomRaw`.
  *   1. Die eingebaute, `private[claude]` `ClaudeAgentBackend.convertTool` bildet JEDES registrierte `AgentTool[F, _]` unconditional auf `Tool.CustomRaw` ab - es gibt dort keinen Zweig, der
  *      stattdessen `Tool.WebSearch` erzeugen könnte. `AgentTool[F, T]` selbst zwingt außerdem zu zwei Dingen: einem JSON-Schema UND einer lokal auszuführenden Funktion (`execute: T => F[String]`).
  *      `web_search` hat aber keins von beidem - kein Schema (siehe Punkt 1) und keine lokale Ausführung (der Server löst das Tool komplett selbst auf, wir bekommen dafür nie einen `ToolCall` zum
  *      Beantworten). Es passt also schlicht nicht in die `AgentTool`-Abstraktion, und die eingebaute Fabrik bietet keinen anderen Erweiterungspunkt an.
  *
  * `AgentBackend[F]` ist dagegen ein öffentliches Trait (siehe sttp-ai-Doku "Interceptors": `LoopAgent` ruft `interceptor.aroundLlmCall(ctx)(agentBackend.sendRequest(...))` auf, unabhängig davon,
  * welche Tools der Backend in den Request packt) - wir können also eine eigene, schlanke Implementierung schreiben, die `Tool.WebSearch.default` direkt (nicht über `AgentTool`) einstreut, und
  * trotzdem den vollen Interceptor-/Budget-/Logging-Mechanismus von sttp-ai nutzen. Diese Klasse ist bewusst eine (kleine) Kopie der Grundidee der eingebauten `ClaudeAgentBackend` - nur mit der
  * zusätzlichen `includeWebSearch`-Fähigkeit.
  *
  * @param client
  *   stateless Request-Builder (siehe `AnthropicClient`)
  * @param model
  *   Modell-Id bzw. Router-Alias, z. B. `"vertex/claude-sonnet-5@eu"`
  * @param includeWebSearch
  *   ob zusätzlich zu den client-seitigen Tools aus `config.userTools` das server-seitige `web_search`-Tool angeboten werden soll
  */
final class ClaudeToolLoopBackend(
    client: ClaudeClient,
    model: String,
    includeWebSearch: Boolean,
    config: AgentConfig[Identity],
) extends AgentBackend[Identity]:

  override val tools: Seq[AgentTool[Identity, ?]] = config.userTools

  override val systemPrompt: Option[String] = config.systemPrompt

  /** Client-seitige Tools werden 1:1 wie in der eingebauten `ClaudeAgentBackend` zu `Tool.CustomRaw` konvertiert (JSON-Schema aus `AgentTool.rawJsonSchema`, defensiv auf ein Objekt-Schema
    * normalisiert). `web_search` wird - falls `includeWebSearch` - zusätzlich vorangestellt; es hat kein `AgentTool`-Gegenstück, da das Modell es nie selbst "ausführt" (kein `ToolCall` in unserer
    * History).
    */
  private val convertedTools: Seq[Tool] =
    (if includeWebSearch then Seq(Tool.WebSearch.default) else Seq.empty) ++ tools.map(convertClientTool)

  private def convertClientTool(tool: AgentTool[Identity, ?]): Tool =
    Tool.CustomRaw(
      name = tool.name,
      description = tool.description,
      inputSchema = ensureObjectType(tool.rawJsonSchema),
    )

  /** Erzwungenes, schemakonformes JSON (Structured Output, siehe `AgentBuilder.deriveResponseSchema`/`ResponseSchema`) - ohne diesen Block würde `config.responseSchema` (von
    * `AnthropicClient.buildStructuredAgent` gesetzt) stillschweigend ignoriert und das Modell könnte frei antworten (z. B. in Markdown-Codefences verpackt oder mit abweichenden Feldnamen) statt exakt
    * dem `ExecutionPlan`-Schema zu folgen.
    */
  private val outputConfig: Option[OutputConfig] =
    config.responseSchema.map(rs => OutputConfig(format = Some(OutputFormat.JsonSchema(rs.schema))))

  /** Kleine, lokale Kopie der gleichnamigen (aber `private[ai]`, also von außerhalb des sttp-ai-Packages nicht nutzbaren) Normalisierung aus `AgentTool`: Provider verlangen für das Tool-Input-Schema
    * ein JSON-Schema vom Typ `object`, manche zusätzlich das `properties`-Feld.
    */
  private def ensureObjectType(schema: Json): Json =
    schema.asObject match
      case Some(obj) =>
        val typed    = if obj.contains("type") then obj else obj.add("type", Json.fromString("object"))
        val repaired =
          if typed("type").contains(Json.fromString("object")) && !typed.contains("properties") then typed.add("properties", Json.obj())
          else typed
        Json.fromJsonObject(repaired)
      case None      => schema

  private def buildMessages(history: ConversationHistory): Seq[Message] =
    history.entries.flatMap {
      case ConversationEntry.UserPrompt(content) => Some(Message.user(content))

      case ConversationEntry.AssistantResponse(content, toolCalls) =>
        val textBlocks    = if content.nonEmpty then List(ContentBlock.Text(content)) else Nil
        val toolUseBlocks = toolCalls.map { tc =>
          val input = io.circe.parser.parse(tc.input).flatMap(_.as[Map[String, Json]]).fold(throw _, identity)
          ContentBlock.ToolUse(tc.id, tc.toolName, input)
        }
        Some(Message.assistant(textBlocks ++ toolUseBlocks))

      case ConversationEntry.ToolResult(toolCallId, _, result) =>
        Some(Message(role = "user", content = List(ContentBlock.ToolResult(toolUseId = toolCallId, content = result, isError = None))))

      case ConversationEntry.IterationMarker(current, max) => Some(Message.user(s"[Iteration $current of $max]"))
    }

  override def sendRequest(
      history: ConversationHistory,
      backend: Backend[Identity],
      includeTools: Boolean,
      iterationInfo: IterationInfo,
  ): Identity[AgentResponse] =
    val request = MessageRequest(
      model = model,
      messages = buildMessages(history).toList,
      system = systemPrompt,
      maxTokens = config.maxTokens.getOrElse(4096),
      tools = if includeTools && convertedTools.nonEmpty then Some(convertedTools.toList) else None,
      outputConfig = outputConfig,
    )

    client.createMessage(request).send(backend).body match
      case Left(error)     => throw new RuntimeException(s"Claude API error: ${error.getMessage}")
      case Right(response) =>
        val textContent = response.content.collectFirst { case ContentBlock.Text(text, _, _) => text }.getOrElse("")
        val toolCalls   = response.content.collect { case ContentBlock.ToolUse(id, name, input) => ToolCall(id, name, Json.fromFields(input).noSpaces) }
        val stopReason  = mapStopReason(response.stopReason)
        val u           = response.usage
        val usage       = TokenUsage(
          inputTokens = Tokens(u.totalInputTokens.toLong),
          outputTokens = Tokens(u.outputTokens.toLong),
          cachedInputTokens = Tokens(u.cacheReadInputTokens.getOrElse(0).toLong),
          reasoningTokens = Tokens.Zero,
          cacheWriteInputTokens = Tokens(u.cacheCreationInputTokens.getOrElse(0).toLong),
        )
        AgentResponse(textContent, toolCalls, stopReason, usage = Some(usage), model = Some(response.model))

  private def mapStopReason(reason: Option[String]): StopReason = reason match
    case Some("end_turn")      => StopReason.EndTurn
    case Some("tool_use")      => StopReason.ToolUse
    case Some("max_tokens")    => StopReason.MaxTokens
    case Some("stop_sequence") => StopReason.StopSequence
    case Some(other)           => StopReason.Other(other)
    case None                  => StopReason.EndTurn
