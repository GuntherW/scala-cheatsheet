package de.codecentric.wittig.scala.tuple

import scala.deriving.Mirror

case class Employee(name: String, number: Int, manager: Boolean)
case class IceCream(name: String, numCherries: Int, inCone: Boolean)

object FromToCaseClass extends App:

  val bob: Employee = Employee("Bob", 42, false)

  // From/To case class from/to tuple
  val bobTuple: (String, Int, Boolean) = Tuple.fromProductTyped(bob)
  val bobAgain: Employee               = summon[Mirror.Of[Employee]].fromProduct(bobTuple)

  println(bobTuple)
  println(bobAgain)

  // Mapping over Tuples
  val bobTupleOption = bobTuple.map[[X] =>> Option[X]]([T] => (t: T) => Some(t))
  println(bobTupleOption)
