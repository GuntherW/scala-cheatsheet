package de.codecentric.wittig.scala.logging.withmdc

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt

import org.slf4j.{LoggerFactory, MDC}

object FuturesWithMDC extends App {

  private val logger = LoggerFactory.getLogger(getClass.getSimpleName)

  logger.warn("eins")
  MDC.put("key1", "### MDC-Wert ###")
  logger.warn(s"zwei ${MDC.get("key1")}")

  private val logWithGlobalExecutionContext =
    given ExecutionContext = scala.concurrent.ExecutionContext.global
    Await.result(
      Future {
        val logger = LoggerFactory.getLogger("ExecutionContext.global")
        logger.warn(s"drei ${MDC.get("key1")}")
      },
      5.seconds
    )

  private val logWithMdcExecutionContext =
    given ExecutionContext = MdcExecutionContext(scala.concurrent.ExecutionContext.global)
    Await.result(
      Future {
        val logger = LoggerFactory.getLogger("MdcExecutionContext")
        logger.warn(s"drei ${MDC.get("key1")}")
      },
      5.seconds
    )
}
