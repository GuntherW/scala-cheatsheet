package de.wittig.scala.magnolia

import de.wittig.scala.magnolia.Print.{given, *}
import magnolia1.*

sealed trait Person derives Print
case class Employee(name: String, alter: Int) extends Person

object Hallo extends App:

  sealed trait A derives Print
  case class B(eins: Int, zwei: Int, drei: Int) extends A

  val b: A = B(1, 2, 3)

  println(1.print)
  println(b.print)
  println(Tree.Branch(Tree.Branch(Tree.Leaf(1), Tree.Leaf(2)), Tree.Leaf(3)).print)

  println(Employee("Name", 3).print)
