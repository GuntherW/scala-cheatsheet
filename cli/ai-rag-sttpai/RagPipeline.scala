package rag

/** Verdrahtet die RAG-Pipeline (siehe README für das Ablaufdiagramm):
  *
  * {{{
  * Nutzerfrage
  *   -> AgentQueryRewriter.rewrite        (LLM: Frage -> stichwortreiche Suchanfrage)
  *   -> HashingTfIdfEmbedder.embed        (Suchanfrage -> Vektor, mit dem bei der Ingestion gefitteten IDF-Modell)
  *   -> VectorStore.search                (pgvector Cosine-Similarity-Suche, Top-K Chunks)
  *   -> AgentRelevanceGrader.grade (je Treffer) (LLM: Chunk relevant? ja/nein)
  *   -> AgentSynthesis.synthesize         (LLM: finale, quellenbelegte Antwort aus den verbleibenden Chunks)
  * }}}
  *
  * Bewusst KEIN Orchestrator-Agent/Planner (wie in `ai-sttpai-agentic`): Die Reihenfolge dieser Schritte ist für JEDE Frage dieselbe (ein fester Workflow, kein LLM entscheidet über den Kontrollfluss) -
  * eine Planning-Phase wäre hier reiner Overhead.
  */
object RagPipeline:

  final case class GradedChunk(hit: VectorStore.Hit, grading: AgentRelevanceGrader.Grading)

  final case class AnswerResult(
      question: String,
      rewrittenQuery: String,
      retrievedHits: List[VectorStore.Hit],
      gradedChunks: List[GradedChunk],
      usedChunks: List[VectorStore.Hit],
      answer: String,
  )

  def answer(question: String, topK: Int = 5): AnswerResult =
    println(s"[RagPipeline] Frage: $question")

    val rewrittenQuery = AgentQueryRewriter.rewrite(question)
    println(s"[RagPipeline] Umformulierte Suchanfrage: $rewrittenQuery")

    given conn: java.sql.Connection     = Db.connect()
    val (retrievedHits, queryEmbedding) =
      try
        val idfModel  = VectorStore
          .loadIdfModel()
          .getOrElse(throw new IllegalStateException("Kein IDF-Modell in der Datenbank gefunden - erst 'scala-cli run . -- ingest' ausführen."))
        val embedding = HashingTfIdfEmbedder.embed(rewrittenQuery, idfModel)
        (VectorStore.search(embedding, topK), embedding)
      finally conn.close()

    println(s"[RagPipeline] ${retrievedHits.size} Treffer aus pgvector (Top-$topK nach Cosine-Distanz):")
    retrievedHits.foreach(h => println(f"[RagPipeline]   ${h.sourceFile}#${h.chunkIndex} distance=${h.distance}%.4f"))

    val gradedChunks = retrievedHits.map { hit =>
      val grading = AgentRelevanceGrader.grade(question, hit.content)
      println(s"[RagPipeline]   Grading ${hit.sourceFile}#${hit.chunkIndex}: relevant=${grading.relevant} (${grading.reasoning})")
      GradedChunk(hit, grading)
    }

    val usedChunks = gradedChunks.filter(_.grading.relevant).map(_.hit)
    val finalUsed  = if usedChunks.nonEmpty then usedChunks else retrievedHits // Fallback: lieber ungefilterter Kontext als gar keiner

    val contextChunks = finalUsed.map(h => AgentSynthesis.ContextChunk(h.sourceFile, h.chunkIndex, h.content))
    val answerText    = AgentSynthesis.synthesize(question, contextChunks)

    AnswerResult(question, rewrittenQuery, retrievedHits, gradedChunks, finalUsed, answerText)
