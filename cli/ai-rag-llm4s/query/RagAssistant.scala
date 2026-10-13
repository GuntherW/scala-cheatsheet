package rag.query

import rag.RagFactory

import org.llm4s.types.Result

object RagAssistant:

  def ask(question: String): Result[String] =
    for
      rag    <- RagFactory.build()
      result <- rag.queryWithAnswer(question)
    yield result.answer
