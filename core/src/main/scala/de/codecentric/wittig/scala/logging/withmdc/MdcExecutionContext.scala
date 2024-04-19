package de.codecentric.wittig.scala.logging.withmdc

import scala.concurrent.ExecutionContext
import org.slf4j.MDC

/** Execution context proxy for propagating MDC from caller thread to execution thread.
  */
class MdcExecutionContext(executionContext: ExecutionContext) extends ExecutionContext:

  override def execute(runnable: Runnable): Unit =
    val callerMdc = MDC.getCopyOfContextMap
    executionContext.execute { () =>
      if (callerMdc != null) MDC.setContextMap(callerMdc)
      try
        runnable.run()
      finally
        MDC.clear()
    }

  override def reportFailure(cause: Throwable): Unit = executionContext.reportFailure(cause)

object MdcExecutionContext:
  def apply(executionContext: ExecutionContext): MdcExecutionContext = new MdcExecutionContext(executionContext)
