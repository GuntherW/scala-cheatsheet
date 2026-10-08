package rag.query

/** Agent 1 der RAG-Pipeline: Formuliert die (ggf. umgangssprachliche, mehrdeutige) Nutzerfrage in eine knappe, stichwortreiche Suchanfrage um, die besser zu den TF-IDF-Embeddings der Vektordatenbank
  * passt (siehe `HashingTfIdfEmbedder` - rein wortbasiert, kein semantisches Verständnis wie ein echtes Embedding-Modell; profitiert daher besonders von expliziten Fachbegriffen statt ganzer Sätze).
  *
  * '''Query Rewriting''': Technik, bei der ein LLM die ursprüngliche Nutzerfrage vor dem eigentlichen Retrieval umformuliert/erweitert (z. B. Synonyme, Fachbegriffe, implizite Begriffe explizit
  * machen), um die Trefferqualität der anschließenden Vektorsuche zu verbessern - unabhängig davon, ob das Embedding-Verfahren selbst "nur" lexikalisch (wie hier) oder semantisch ist.
  */
object AgentQueryRewriter:

  private val systemPrompt =
    """Du bist der Query-Rewriter einer RAG-Pipeline (Retrieval-Augmented Generation).
      |Die nachgelagerte Suche basiert auf TF-IDF (reiner Wortabgleich, KEIN semantisches Verständnis).
      |
      |Aufgabe: Formuliere die Nutzerfrage in eine kurze, stichwortreiche Suchanfrage um.
      |Regeln:
      |- Nenne die wichtigsten Fachbegriffe der Frage explizit (keine Füllwörter, keine ganzen Sätze).
      |- Ergänze naheliegende Synonyme/verwandte Fachbegriffe, falls hilfreich.
      |- Antworte NUR mit der Suchanfrage selbst, ohne Erklärung, ohne Anführungszeichen.
      |""".stripMargin

  def rewrite(question: String): String =
    val agent = AnthropicClient.buildAgent("QueryRewriter", systemPrompt, maxTokens = 200)
    AnthropicClient.run(agent, question).trim
