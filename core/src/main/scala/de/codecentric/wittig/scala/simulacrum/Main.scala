package de.codecentric.wittig.scala.simulacrum

case class Person(name: String, alter: Int)

object Main extends App {
  import Printable.ops._

  val p = Person("Gunther", 22)

  println(s"Hallo ${p.asString}")
  println(s"Hallo ${"Gunther".asString}")
}
