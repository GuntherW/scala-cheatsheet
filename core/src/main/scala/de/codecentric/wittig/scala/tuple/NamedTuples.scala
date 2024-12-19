package de.codecentric.wittig.scala.tuple

import scala.language.experimental.namedTuples
import scala.util.chaining.*

// Named Tuple
type Person = (name: String, age: Int)

object NamedTuples extends App:

  private val bob: Person = (name = "Bob", age = 33)
  bob match
    case (name, 33)  => println(s"Hallo $name, matched 33")
    case (name, age) => println(s"Hallo $name, $age")

  private val alice = ("Alice", 22)
  private val steve = ("Steve", 25)

  private val persons: List[Person] = List(bob, alice, steve)

  persons
    .filter(_.age > 22)
    .mkString(", ")
    .tap(println)
