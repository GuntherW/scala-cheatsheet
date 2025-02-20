package de.wittig.macros.baeldung.getclassnames

import de.wittig.macros.baeldung.getclassnames.ClazzNames.*

import scala.util.chaining.scalaUtilChainingOps

object Main extends App:

  case class Person(name: String)

  getType1("ein string").tap(println)
  getType2("ein string").tap(println)
  getType1(Person("peter")).tap(println)
  getType2(Person("peter")).tap(println)
