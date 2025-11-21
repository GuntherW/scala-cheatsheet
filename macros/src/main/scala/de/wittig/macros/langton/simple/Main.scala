package de.wittig.macros.langton.simple

import DebugMacro.*

@main
def main(): Unit =

  val greeting = "Hallo"

  println("-" * 10)
  println(debug("Hallo"))
  println(debug(greeting))

  val deb = debug(greeting)
  println("-" * 10)
  println(deb)
  println(debug(deb))
