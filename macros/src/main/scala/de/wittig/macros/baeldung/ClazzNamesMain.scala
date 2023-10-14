package de.wittig.macros.baeldung

import scala.util.chaining.scalaUtilChainingOps
import ClazzNames.*

object ClazzNamesMain extends App:

  case class Person(name: String)

  getTypeMacro1("ein string").tap(println)
  getTypeMacro2("ein string").tap(println)
  getTypeMacro1(Person("peter")).tap(println)
  getTypeMacro2(Person("peter")).tap(println)
