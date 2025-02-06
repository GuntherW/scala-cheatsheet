package de.wittig.macros.rockthejvm.macrosUsage

import de.wittig.macros.rockthejvm.macros.KStructuralTypes.*

object KStructuralTypes extends App:

  val simpleRecord = Record.make(
    "name"        -> "Alice",
    "age"         -> 30.5,
    "favLanguage" -> "Scala"
  )

  val name = simpleRecord.name
  println(name)
