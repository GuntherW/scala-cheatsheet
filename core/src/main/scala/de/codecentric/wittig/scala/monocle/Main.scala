package de.codecentric.wittig.scala.monocle
import monocle.syntax.all.*
import scala.util.chaining.*

object Main extends App:

  val anna = User("Anna", 40, Address(12, "high street"))

  anna
    .focus(_.name)
    .replace("Bob")
    .tap(println)

case class User(name: String, age: Int, address: Address)

case class Address(streetNumber: Int, streetName: String)
