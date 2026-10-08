package rag

/** Orchestriert die Ingestion-Phase (einmalig vor dem eigentlichen Fragen-Beantworten auszuführen, siehe `Main.scala`, Subcommand `ingest`):
  *
  * {{{
  * PDFs (docs-Ordner)
  *   -> PdfTextExtractor.extractText (Text pro PDF)
  *   -> Chunking.chunk (überlappende Chunks pro PDF)
  *   -> HashingTfIdfEmbedder.fitIdf (EIN IDF-Modell über ALLE Chunks aller PDFs)
  *   -> HashingTfIdfEmbedder.embed (ein Vektor pro Chunk, mit besagtem IDF-Modell)
  *   -> VectorStore.insertChunk / VectorStore.saveIdfModel (Persistierung in Postgres/pgvector)
  * }}}
  *
  * Das IDF-Modell wird bewusst erst NACH dem Sammeln ALLER Chunks gefittet (nicht PDF-für-PDF) - sonst würde jedes PDF mit einem eigenen, nicht vergleichbaren Vektorraum enden.
  */
object Ingestion:

  final case class IngestedChunk(sourceFile: String, chunkIndex: Int, content: String)

  def run(docsDir: os.Path, embeddingDims: Int = 512): Unit =
    val pdfFiles = os.list(docsDir).filter(_.ext == "pdf").sorted
    if pdfFiles.isEmpty then
      println(s"[Ingestion] Keine PDFs in $docsDir gefunden - nichts zu tun.")
    else
      println(s"[Ingestion] Gefundene PDFs: ${pdfFiles.map(_.last).mkString(", ")}")

      val allChunks = pdfFiles.flatMap { pdf =>
        val text   = PdfTextExtractor.extractText(pdf)
        val chunks = Chunking.chunk(text)
        println(s"[Ingestion]   ${pdf.last}: ${chunks.size} Chunk(s)")
        chunks.zipWithIndex.map { case (content, idx) => IngestedChunk(pdf.last, idx, content) }
      }

      println(s"[Ingestion] Fitte IDF-Modell über ${allChunks.size} Chunk(s) insgesamt (dims=$embeddingDims)...")
      val idfModel = HashingTfIdfEmbedder.fitIdf(allChunks.map(_.content), embeddingDims)

      given conn: java.sql.Connection = Db.connect()
      try
        VectorStore.clearAll()
        VectorStore.saveIdfModel(idfModel)
        allChunks.foreach { c =>
          val embedding = HashingTfIdfEmbedder.embed(c.content, idfModel)
          VectorStore.insertChunk(c.sourceFile, c.chunkIndex, c.content, embedding)
        }
        println(s"[Ingestion] ${allChunks.size} Chunk(s) in pgvector gespeichert.")
      finally conn.close()
