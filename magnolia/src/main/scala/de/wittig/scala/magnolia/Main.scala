package de.wittig.scala.magnolia

import de.wittig.scala.magnolia.Print.{*, given}
import magnolia1.*

sealed trait Person derives Print
case class Employee(name: String, alter: Int, entfernung: Double) extends Person

@main
def hallo(): Unit =

  println(1.print)
  println(Tree.Branch(Tree.Branch(Tree.Leaf(1), Tree.Leaf(2)), Tree.Leaf(3)).print)
  println(Employee("Name", 3, 5.14).print)
