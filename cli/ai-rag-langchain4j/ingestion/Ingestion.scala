package rag.ingestion

import rag.{EmbeddingModels, PgConfig, VectorStore}

import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor

import scala.jdk.CollectionConverters.*

/** Lädt alle PDFs aus `docs/`, zerlegt sie in Chunks und schreibt deren Embeddings in pgvector - die komplette Indexierungsphase einer RAG-Pipeline, hier fast vollständig durch langchain4j-
  * Bordmittel abgedeckt (vgl. `ai-rag-sttpai/Ingestion.scala`, wo PDF-Parsing, Chunking und Embedding noch von Hand implementiert sind).
  *
  * Liegt bewusst in einem eigenen Unterordner (`ingestion/`) getrennt von der Abfrageseite (`query/`) - beide Seiten einer RAG-Pipeline haben kaum Berührungspunkte (außer dem gemeinsam genutzten
  * Embedding-Modell und Vektorstore im Projekt-Root) und lassen sich dadurch unabhängig voneinander lesen.
  */
object Ingestion:

  /** Chunk-Größe und Overlap in Zeichen (nicht Tokens) - `DocumentSplitters.recursive` versucht zunächst an Absatz-, dann an Satz-, dann an Wortgrenzen zu trennen und erst als letzten Ausweg mitten
    * im Wort, damit möglichst wenig Kontext an den Chunk-Grenzen verloren geht.
    */
  private val chunkSize    = 500
  private val chunkOverlap = 50

  def run(docsDir: os.Path): Unit =
    val pdfFiles = os.list(docsDir).filter(_.ext == "pdf").sorted
    require(pdfFiles.nonEmpty, s"Keine PDFs in $docsDir gefunden - zuerst 'generate-docs' ausführen.")

    val documents: List[Document] = pdfFiles.map { path =>
      val document = FileSystemDocumentLoader.loadDocument(path.toNIO, new ApachePdfBoxDocumentParser())
      // FileSystemDocumentLoader setzt bereits `file_name`/`absolute_directory_path` - hier zusätzlich ein kurzer, stabiler Schlüsselname für die spätere Ausgabe der Quellen (siehe RagAssistant).
      document.metadata().put("source_file", path.last)
      document
    }.toList

    val embeddingStore = VectorStore.build(dropTableFirst = true)
    val splitter       = DocumentSplitters.recursive(chunkSize, chunkOverlap)

    val ingestor = EmbeddingStoreIngestor
      .builder()
      .documentSplitter(splitter)
      .embeddingModel(EmbeddingModels.allMiniLm)
      .embeddingStore(embeddingStore)
      .build()

    ingestor.ingest(documents.asJava)
    println(s"[Ingestion] ${documents.size} PDFs aus $docsDir eingelesen und in Tabelle '${PgConfig.table}' geschrieben.")
