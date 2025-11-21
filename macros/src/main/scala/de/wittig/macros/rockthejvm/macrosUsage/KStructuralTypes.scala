package de.wittig.macros.rockthejvm.macrosUsage

import de.wittig.macros.rockthejvm.macros.KStructuralTypes.*

@main
def kStructuralTypes(): Unit =

  val simpleRecord = Record.make(
    "name"        -> "Alice",
    "age"         -> 30.5,
    "favLanguage" -> "Scala"
  )

  val name = simpleRecord.name
  println(name)
