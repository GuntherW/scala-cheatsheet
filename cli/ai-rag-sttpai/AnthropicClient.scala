package rag

import io.circe.Codec
import sttp.ai.claude.ClaudeClient
import sttp.ai.claude.agent.ClaudeAgent
import sttp.ai.claude.config.ClaudeConfig
import sttp.ai.core.agent.*
import sttp.ai.core.http.RetryingBackend
import sttp.client4.{DefaultSyncBackend, SyncBackend}
import sttp.model.Uri
import sttp.shared.Identity
import sttp.tapir.Schema

/** Dünner Wrapper um den `ClaudeClient` aus [[https://sttp-ai.softwaremill.com/ sttp-ai]] (Modul `claude`) - identisches Setup wie in `ai-sttpai-agentic`/`ai-sttpai-manual` (Requesty-Router statt der
  * offiziellen Anthropic-Basis-URL, Modell `vertex/claude-sonnet-5@eu`). Dieses Projekt braucht keine Tools/Interceptoren/Usage-Tracking - nur einfache, strukturierte Einzel-Requests für die drei
  * RAG-Agenten (Query-Rewriter, Relevance-Grader, Synthesis), daher ein bewusst schlankeres Setup ohne `Interceptors.scala`.
  */
object AnthropicClient:

  val Model = "vertex/claude-sonnet-5@eu"

  private val AnthropicVersion = "2023-06-01"

  private val apiKey: String = Env.get("ANTHROPIC_AUTH_TOKEN")
    .orElse(Env.get("ANTHROPIC_API_KEY"))
    .getOrElse(throw new RuntimeException("Weder ANTHROPIC_AUTH_TOKEN noch ANTHROPIC_API_KEY gesetzt."))

  private val config = ClaudeConfig(
    apiKey = apiKey,
    anthropicVersion = AnthropicVersion,
    baseUrl = Uri.unsafeParse("https://router.eu.requesty.ai"),
  )

  private val client: ClaudeClient = ClaudeClient(config)

  private val httpBackend: SyncBackend = RetryingBackend(DefaultSyncBackend(), config.maxRetries)

  def close(): Unit = httpBackend.close()

  def backend: SyncBackend = httpBackend

  /** Einfacher Text-Agent (ein Request, kein Tool-Use-Loop) - genutzt vom Query-Rewriter und Synthesis-Agent. */
  def buildAgent(caller: String, systemPrompt: String, maxTokens: Int = 1500): Agent[Identity, String, String] =
    ClaudeAgent
      .synchronous(client, Model)
      .maxIterations(1)
      .systemPrompt(systemPrompt)
      .maxTokens(maxTokens)
      .interceptors(Seq(Interceptors.loggingFor(caller)))
      .build

  /** Agent mit erzwungenem, schemakonformem JSON-Output (Structured Output) - genutzt vom Relevance-Grader. */
  def buildStructuredAgent[T: {Schema, Codec}](caller: String, systemPrompt: String): Agent[Identity, String, T] =
    ClaudeAgent
      .synchronous(client, Model)
      .maxIterations(1)
      .systemPrompt(systemPrompt)
      .interceptors(Seq(Interceptors.loggingFor(caller)))
      .deriveResponseSchema[T]
      .build

  /** Führt einen Agenten mit einer einzelnen Nutzernachricht aus (keine Historie über mehrere Aufrufe hinweg nötig - jeder RAG-Agent in diesem Projekt ist zustandslos pro Aufruf). */
  def run[T](agent: Agent[Identity, String, T], userMessage: String): T =
    agent.run(userMessage)(backend).finalAnswer match
      case Right(result) => result
      case Left(failure) => throw new RuntimeException(s"Agent-Aufruf fehlgeschlagen: $failure")
