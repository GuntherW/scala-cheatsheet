package de.codecentric.wittig.scala.tuple

object Main extends App {
  private val input: (Int, Int, Int)           = (1, 2, 3)
  private def sum(a: Int, b: Int, c: Int): Int = a + b + c

  // how to use a method with a Tuple, when it was not originally written for:
  private val summe = (sum _).tupled(input)

  println(summe)

  // how to use a method with Currying, when it was not originally written for:
  private val c: Int => Int => Int => Int = (sum _).curried
  private val c1: Int => Int => Int       = (sum _).curried(1)
  private val c2: Int => Int              = (sum _).curried(1)(2)
  private val c3: Int                     = (sum _).curried(1)(2)(3)
}
