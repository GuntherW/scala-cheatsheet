package de.codecentric.wittig.scala.contextualabstractions

@main
def contextFunctions(): Unit =

  class TimeProvider():
    def sagHallo(): String = "Hallo"

  given TimeProvider = new TimeProvider()

  /** ?=> */
  val contextFunction: TimeProvider ?=> String = summon[TimeProvider].sagHallo().reverse

  println(contextFunction)
