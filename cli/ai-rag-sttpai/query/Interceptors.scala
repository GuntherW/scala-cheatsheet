package rag.query

import sttp.ai.core.agent.interceptor.{LogLevel, LoggingInterceptor}
import sttp.monad.IdentityMonad
import sttp.shared.Identity

private given sttp.monad.MonadError[Identity] = IdentityMonad

/** Minimaler Interceptor-Baustein - nur Logging, kein Usage-/Budget-Tracking (im Unterschied zu `ai-sttpai-agentic/Interceptors.scala`), da dieses Projekt pro Nutzeranfrage nur drei einzelne, kurze
  * LLM-Calls macht (Query-Rewriter, Relevance-Grader, Synthesis) statt eines Multi-Agenten-Workflows mit Tool-Use-Loops.
  */
object Interceptors:

  private def truncate(s: String, maxLen: Int = 300): String =
    val flattened = Option(s).getOrElse("").replaceAll("\\s+", " ").trim
    if flattened.length > maxLen then flattened.take(maxLen) + "…" else flattened

  def loggingFor(caller: String): LoggingInterceptor[Identity] =
    LoggingInterceptor[Identity] { (level, msg) =>
      val tag = level match
        case LogLevel.Debug => "debug"
        case LogLevel.Info  => "info"
        case LogLevel.Warn  => "WARN"
      println(s"[LLM:$caller/$tag] ${truncate(msg)}")
    }
