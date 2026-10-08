package rag.query

import rag.{EmbeddingModels, VectorStore}

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.service.{AiServices, SystemMessage}
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.store.embedding.EmbeddingStore

/** Der eigentliche RAG-Assistent als `AiServices`-Interface: langchain4j generiert zur Laufzeit eine Implementierung, die bei jedem Aufruf von `answer(...)` automatisch
  *   1. die Frage einbettet und über den `ContentRetriever` die ähnlichsten Chunks aus pgvector holt,
  *   1. diese Chunks zusammen mit der System-/Nutzerfrage an das LLM schickt,
  *   1. die vom LLM generierte Antwort zurückgibt.
  *
  * Das entspricht genau der dreistufigen, von Hand geschriebenen Pipeline aus `ai-rag-sttpai` (`RagPipeline.scala`) - hier übernimmt `AiServices` Prompt-Bau und Retrieval-Verdrahtung.
  *
  * Liegt bewusst in einem eigenen Unterordner (`query/`) getrennt von der Indexierungsseite (`ingestion/`) - wer nur verstehen will, wie eine Frage beantwortet wird, muss sich nicht erst durch
  * PDF-Parsing/Chunking/Ingestion-Code wühlen.
  */
trait RagAssistant:

  @SystemMessage(Array(
    "Du bist ein hilfreicher Assistent, der Fragen ausschließlich auf Basis der bereitgestellten Dokumentauszüge beantwortet.",
    "Wenn die Antwort nicht eindeutig aus den Auszügen hervorgeht, sage das ehrlich, statt zu raten oder dein allgemeines Weltwissen zu nutzen.",
    "Antworte knapp und auf Deutsch.",
  ))
  def answer(question: String): String

object RagAssistant:

  def create(embeddingStore: EmbeddingStore[TextSegment], maxResults: Int = 3): RagAssistant =
    val contentRetriever = EmbeddingStoreContentRetriever
      .builder()
      .embeddingStore(embeddingStore)
      .embeddingModel(EmbeddingModels.allMiniLm)
      .maxResults(maxResults)
      .build()

    AiServices
      .builder(classOf[RagAssistant])
      .chatModel(LlmConfig.chatModel())
      .contentRetriever(contentRetriever)
      .build()

  /** Beantwortet eine einzelne Nutzerfrage auf Basis der zuvor indexierten Dokumente (siehe `ingestion/Ingestion.scala`) - baut dafür den `EmbeddingStore` auf die bestehende pgvector-Tabelle auf
    * (`dropTableFirst = false`, im Unterschied zur Ingestion-Seite bleibt der Inhalt hier unangetastet) und erzeugt daraus einen `RagAssistant` für genau diesen einen Aufruf.
    */
  def ask(question: String): String =
    val embeddingStore = VectorStore.build(dropTableFirst = false)
    val assistant      = create(embeddingStore)
    assistant.answer(question)
