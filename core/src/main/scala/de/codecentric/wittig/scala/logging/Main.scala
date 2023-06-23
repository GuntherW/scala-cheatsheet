package de.codecentric.wittig.scala.logging

import org.slf4j.{Logger, LoggerFactory}

object Main extends App:
  val c1 = MeineTestKlasse1()
  c1.logWithDuplicateMessageFilter()

class MeineTestKlasse1:
  private val logger = LoggerFactory.getLogger(classOf[MeineTestKlasse1])

  def logWithDuplicateMessageFilter(): Unit =
    val cachedSize = 50 // wird in logback <CachedSize> property definiert. Default: 100
    (1 to 100).foreach { i =>
      logger.warn("Hallo warn") // wird nur drei mal geloggt, wegen <AllowedRepetitions> in logback.xml
    }
    (1 to 100).foreach { i =>
      logger.warn(s"Hallo {}", i) // wird nur drei mal geloggt, wegen <AllowedRepetitions> in logback.xml
    }
    (1 to cachedSize).foreach { i =>
      logger.warn(s"Hallo $i") // wird komplett geloggt
    }
    logger.warn("Hallo warn")
