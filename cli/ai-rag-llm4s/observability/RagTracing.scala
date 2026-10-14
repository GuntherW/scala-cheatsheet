package rag.observability

import org.llm4s.rag.RAGAnswerResult
import org.llm4s.trace.{TraceEvent, Tracing}

/** Ergänzende Trace-Events für das, was `RAG` in llm4s 0.4.1 NICHT selbst meldet.
  *
  * `RAG` (mit `withTracing`) meldet nur die Embedding-Nutzung. Der LLM-Call der Antwortgenerierung und die Gesamtdauer fehlen - sie werden
  * hier nach `queryWithAnswer` manuell an denselben Tracer gemeldet und landen dadurch im OTel-Export (OpenObserve).
  * Tracing-Fehler werden bewusst ignoriert: Observability darf die Anwendung nie kaputtmachen.
  */
object RagTracing:

  def traceAnswer(tracer: Tracing, model: String, result: RAGAnswerResult, durationMs: Long): Unit =
    val usage = result.usage
    tracer.traceEvent(TraceEvent.CustomEvent("rag.retrieved", ujson.Obj("question" -> result.question, "chunks" -> result.contexts.size)))
    usage.foreach(u => tracer.traceTokenUsage(u, model, "rag-answer"))
    tracer.traceRAGOperation(
      operation           = "answer",
      durationMs          = durationMs,
      llmPromptTokens     = usage.map(_.promptTokens),
      llmCompletionTokens = usage.map(_.completionTokens)
    )
    ()
