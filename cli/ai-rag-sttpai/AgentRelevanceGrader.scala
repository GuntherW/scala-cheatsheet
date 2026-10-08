package rag

import io.circe.Codec
import sttp.tapir.Schema

/** Agent 2 der RAG-Pipeline: Bewertet für jeden per Vektorsuche gefundenen Chunk, ob er tatsächlich zur Beantwortung der Nutzerfrage relevant ist, und filtert irrelevante Treffer heraus.
  *
  * '''Relevance Grading / Reranking''': Die reine Vektor-Ähnlichkeit (hier: Cosine-Distanz von TF-IDF-Vektoren) ist nur eine lexikalische Näherung - ein Chunk kann viele der gesuchten Wörter
  * enthalten, ohne inhaltlich zur Frage zu passen (oder umgekehrt). Ein LLM, das Frage UND Chunk-Text gemeinsam sieht, kann diese Fehleinschätzungen korrigieren, bevor der Chunk als Kontext an den
  * Synthesis-Agent weitergereicht wird - ein zusätzlicher, inhaltlicher Qualitäts-Check nach dem rein mathematischen Retrieval-Schritt.
  *
  * Technisch über Anthropics natives '''Structured Output''' gelöst (siehe `AnthropicClient.buildStructuredAgent`, analog zu `ExecutionPlan` in `ai-sttpai-agentic`) - das Modell MUSS exakt dem
  * `Grading`-Schema entsprechend antworten, ein Multi-Turn-Loop ist dafür nicht nötig.
  */
object AgentRelevanceGrader:

  /** @param relevant
    *   `true`, falls der Chunk zur Beantwortung der Frage beiträgt.
    * @param reasoning
    *   kurze Begründung - dient nur der Nachvollziehbarkeit/Logging.
    */
  final case class Grading(relevant: Boolean, reasoning: String) derives Schema, Codec

  private def systemPrompt(question: String, chunk: String) =
    s"""Du bist der Relevanz-Prüfer einer RAG-Pipeline.
       |Beurteile, ob der folgende Textauszug zur Beantwortung der Nutzerfrage beiträgt.
       |
       |Nutzerfrage: "$question"
       |
       |Textauszug:
       |\"\"\"
       |$chunk
       |\"\"\"
       |""".stripMargin

  def grade(question: String, chunk: String): Grading =
    val agent = AnthropicClient.buildStructuredAgent[Grading]("RelevanceGrader", systemPrompt(question, chunk))
    AnthropicClient.run(agent, "Bewerte diesen Textauszug.")
