package rag.ingestion

import rag.{PgConfig, RagFactory}

import org.llm4s.types.Result

object Ingestion:

  def run(docsDir: os.Path): Result[Int] =
    val pdfFiles = os.list(docsDir).filter(_.ext == "pdf").sorted
    require(pdfFiles.nonEmpty, s"Keine PDFs in $docsDir gefunden - zuerst 'generate-docs' ausführen.")

    for
      rag    <- RagFactory.build()
      _      <- rag.clear()
      counts <- pdfFiles.foldLeft[Result[Int]](Right(0)) { (acc, path) =>
        for
          total <- acc
          n     <- rag.ingest(path.toNIO)
          _      = println(s"[Ingestion] ${path.last}: $n Chunks")
        yield total + n
      }
      _ = println(s"[Ingestion] ${pdfFiles.size} PDFs, $counts Chunks in Tabelle '${PgConfig.table}' geschrieben.")
    yield counts
