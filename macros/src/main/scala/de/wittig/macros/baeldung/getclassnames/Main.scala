package de.wittig.macros.baeldung.getclassnames

import de.wittig.macros.baeldung.getclassnames.ClazzNames.*

import scala.util.chaining.scalaUtilChainingOps

object Main extends App:

  case class Person(name: String)

  getTypeMacro1("ein string").tap(println)
  getTypeMacro2("ein string").tap(println)
  getTypeMacro1(Person("peter")).tap(println)
  getTypeMacro2(Person("peter")).tap(println)
