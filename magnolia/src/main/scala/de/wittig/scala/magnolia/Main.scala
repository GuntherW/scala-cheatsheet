package de.wittig.scala.magnolia

import de.wittig.scala.magnolia.Print.*
import magnolia1.*

sealed trait Person
case class Employee(name: String, alter: Int) extends Person
case class Employer(name: String, alter: Int) extends Person

object Hallo extends App {

  sealed trait A derives Print
  case class B(eins: Int, zwei: Int, drei: Int) extends A

  val b: A = B(1, 2, 3)

  println("Hallo Welt")
  println("Hallo Welt!")
  println(1.print)
  println(b.print)
}
