package rag.query

import rag.RagFactory
import rag.observability.{Observability, RagTracing}

import org.llm4s.types.Result

object RagAssistant:

  def ask(question: String, obs: Observability): Result[String] =
    for
      rag <- RagFactory.build(obs)
      start  = System.nanoTime
      result <- rag.queryWithAnswer(question)
      _ = RagTracing.traceAnswer(obs.tracer, RagFactory.chatModel, result, (System.nanoTime - start) / 1_000_000)
    yield result.answer
