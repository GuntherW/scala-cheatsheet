package de.wittig.macros.langton.simple

import DebugMacro.*

object Main extends App {

  private val greeting = "Hallo"
  println(debug("Hallo"))
  println(debug(greeting))

  private val deb = debug(greeting)
  println(deb)
  println(debug(deb))
}
