package rag

/** Agent 3 (Aggregator/Finalschritt) der RAG-Pipeline: Erhält die Nutzerfrage sowie die als relevant eingestuften Chunks (`AgentRelevanceGrader`) als Kontext und erzeugt daraus die finale Antwort -
  * inklusive Quellenangaben, damit nachvollziehbar bleibt, aus welchem PDF/Chunk welche Information stammt (Grounding).
  *
  * '''Context Passing''': Die Ausgaben vorheriger Pipeline-Schritte (hier: die gefilterten Chunk-Texte) werden als Teil des Prompts in den nächsten Schritt eingebettet, statt dem Modell freies
  * (Vor-)Wissen zu überlassen - das reduziert Halluzinationen, weil Behauptungen sich auf tatsächlich vorgelegten Text stützen können (Grounding).
  */
object AgentSynthesis:

  private val systemPrompt =
    """Du bist der Synthesis-Agent einer RAG-Pipeline (Retrieval-Augmented Generation).
      |Du bekommst eine Nutzerfrage sowie mehrere als relevant eingestufte Textauszüge aus lokalen PDF-Dokumenten.
      |
      |Regeln:
      |- Beantworte die Frage AUSSCHLIESSLICH auf Basis der gegebenen Textauszüge.
      |- Falls die Auszüge die Frage nicht beantworten, sage das explizit - erfinde NICHTS dazu.
      |- Gib am Ende eine Liste der genutzten Quellen an (Dateiname + Chunk-Nummer).
      |- Antworte auf Deutsch, klar und knapp.
      |""".stripMargin

  final case class ContextChunk(sourceFile: String, chunkIndex: Int, content: String)

  private def renderContext(chunks: Seq[ContextChunk]): String =
    chunks
      .map(c => s"[Quelle: ${c.sourceFile}#${c.chunkIndex}]\n${c.content}")
      .mkString("\n\n---\n\n")

  def synthesize(question: String, chunks: Seq[ContextChunk]): String =
    val agent       = AnthropicClient.buildAgent("Synthesis", systemPrompt, maxTokens = 1500)
    val userMessage =
      s"""Nutzerfrage: $question
         |
         |Relevante Textauszüge:
         |${renderContext(chunks)}
         |""".stripMargin
    AnthropicClient.run(agent, userMessage)
