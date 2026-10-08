package rag

import rag.ingestion.{GenerateSampleDocs, Ingestion}
import rag.query.RagAssistant

/** Einstiegspunkt dieser Lernpipeline - drei Subcommands:
  *   - `generate-docs`: erzeugt die 10 Beispiel-PDFs in `docs/` (einmalig, oder erneut bei geänderten Inhalten in `ingestion/SampleDocs.scala`).
  *   - `ingest`: liest alle PDFs aus `docs/`, chunked/embedded sie und schreibt sie in pgvector (siehe `ingestion/Ingestion.scala`). Leert vorher die Tabelle, mehrfaches Ausführen ist also gefahrlos.
  *   - `ask "<Frage>"`: beantwortet eine Frage auf Basis der zuvor indexierten Dokumente (siehe `query/RagAssistant.scala`).
  *
  * Voraussetzung für `ingest`/`ask`: der `postgres-rag`-Container läuft (`cd docker && docker compose up -d postgres-rag`).
  */
object Main:

  private val docsDir = os.pwd / "docs"

  def main(args: Array[String]): Unit =
    args.toList match
      case "generate-docs" :: Nil   => GenerateSampleDocs.run(docsDir)
      case "ingest" :: Nil          => Ingestion.run(docsDir)
      case "ask" :: question :: Nil =>
        val embeddingStore = VectorStore.build(dropTableFirst = false)
        val assistant      = RagAssistant.create(embeddingStore)
        println(assistant.answer(question))
      case _                        => println("""Verwendung:
                                                 |  scala-cli run . -- generate-docs       # erzeugt die 10 Beispiel-PDFs in docs/
                                                 |  scala-cli run . -- ingest               # indexiert docs/ in pgvector
                                                 |  scala-cli run . -- ask "<Frage>"        # beantwortet eine Frage auf Basis der indexierten Dokumente
                                                 |""".stripMargin)
