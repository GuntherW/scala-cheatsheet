package de.codecentric.wittig.scala.monocle
import monocle.Prism
import scala.util.chaining.*

@main
def prisms(): Unit =

  // define a prism for JStr => Option[String]
  val jStr  = Prism[Building, String] {
    case House(v) => Some(v)
    case _        => None
  }(House.apply)
  // define the same prism with partial
  val jStr2 = Prism.partial[Building, String] {
    case House(v) => v
  }(House.apply)

  jStr("hello").tap(println)                          // House(hello)
  jStr.getOption(House("Hello")).tap(println)         // Some(Hello)
  jStr.getOption(Garage(3.2)).tap(println)            // None
  jStr.replace("Bar")(House("Hello")).tap(println)    // House(Bar)
  jStr.modify(_.reverse)(House("Hello")).tap(println) // House(olleH)

sealed trait Building
case class House(name: String)             extends Building
case class Garage(length: Double)          extends Building
case class Sonst(v: Map[String, Building]) extends Building
