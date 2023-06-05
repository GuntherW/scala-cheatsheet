package de.codecentric.wittig.scala.contextualabstractions

import de.codecentric.wittig.scala.contextualabstractions.ContextFunctions.TimeProvider

object ContextFunctions extends App:

  class TimeProvider():
    def sagHallo(): String = "Hallo"

  given TimeProvider = new TimeProvider()

  /** ?=> */
  val contextFunction: TimeProvider ?=> String = summon[TimeProvider].sagHallo().reverse

  println(contextFunction)
