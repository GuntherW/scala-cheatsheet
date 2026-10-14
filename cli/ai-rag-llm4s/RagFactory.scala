package rag

import rag.observability.Observability

import org.llm4s.chunking.ChunkerFactory
import org.llm4s.config.Llm4sConfig
import org.llm4s.llmconnect.LLMConnect
import org.llm4s.model.ModelRegistryService
import org.llm4s.rag.{EmbeddingProvider, RAG}
import org.llm4s.rag.RAG.RAGConfigOps
import org.llm4s.types.Result

object RagFactory:

  val systemPrompt: String =
    """Du bist ein hilfreicher Assistent, der Fragen ausschließlich auf Basis der bereitgestellten Dokumentauszüge beantwortet.
      |Wenn die Antwort nicht eindeutig aus den Auszügen hervorgeht, sage das ehrlich, statt zu raten oder dein allgemeines Weltwissen zu nutzen.
      |Antworte knapp und auf Deutsch.""".stripMargin

  val chunkSize    = 500
  val chunkOverlap = 50
  val topK         = 3

  val chatModel           = "vertex/claude-sonnet-5-5@eu" // muss zu resources/application.conf passen
  val embeddingModel      = "azure/openai/text-embedding-3-small@francecentral"
  val embeddingDimensions = 1536

  def build(obs: Observability): Result[RAG] =
    for
      providerConfig <- Llm4sConfig.defaultProvider()
      registry       <- Llm4sConfig.modelRegistryService()
      given ModelRegistryService = registry
      client     <- LLMConnect.getClient(providerConfig)
      _          <- client.validate() // früh scheitern bei falscher Provider-Konfiguration
      embeddings <- Llm4sConfig.embeddings()
      (_, embeddingConfig) = embeddings
      rag <- RAG
        .builder()
        .withEmbeddings(EmbeddingProvider.OpenAI, embeddingModel, embeddingDimensions)
        .withChunking(ChunkerFactory.Strategy.Sentence, chunkSize, chunkOverlap)
        .withPgVector(PgConfig.jdbcUrl, PgConfig.user, PgConfig.password, PgConfig.table)
        .withTopK(topK)
        .withLLM(client)
        .withSystemPrompt(systemPrompt)
        .withTracing(obs.tracer)
        .build(_ => Right(embeddingConfig))
    yield rag
