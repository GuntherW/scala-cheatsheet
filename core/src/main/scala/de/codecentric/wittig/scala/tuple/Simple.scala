package de.codecentric.wittig.scala.tuple

object Simple extends App:

  private val input: (Int, Int, Int)           = (1, 2, 3)
  private def sum(a: Int, b: Int, c: Int): Int = a + b + c

  // how to use a method with a Tuple, when it was not originally written for:
  private val summe = sum.tupled(input)

  println(summe)

  // how to use a method with Currying, when it was not originally written for:
  val c: Int => Int => Int => Int = sum.curried
  val c1: Int => Int => Int       = sum.curried(1)
  val c2: Int => Int              = sum.curried(1)(2)
  val c3: Int                     = sum.curried(1)(2)(3)
