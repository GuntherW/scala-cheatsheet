package de.codecentric.wittig.scala.refined

import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric._
object Main extends App {

  /** Refined for Literals at CompileTime */
  val ct: Int Refined Positive = 5
//  val ctFailing: Int Refined Positive = -5
  println(ct)

  val ct2 = refineMV[Positive](5)
//  val ct2Failing = refineMV[Positive](-5)

  /** Refined at Runtime */
  val x   = 5
  val rt1 = refineV[Positive](x) // Right(5)
  val rt2 = refineV[Positive](-x) // Left("Predicate failed: (-5>0).")
  println(rt1)
  println(rt2)
}
