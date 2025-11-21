package de.wittig.macros.rockthejvm.macrosUsage
import de.wittig.macros.rockthejvm.macros.DQuoteMatching.*
import scala.util.chaining.scalaUtilChainingOps

object DQuoteMatching extends App:
  pmOptions(Some(42)).tap(println)
  pmOptions(Some(43)).tap(println)
  pmOptions(Option(42)).tap(println)   // Option(42) != Some(42))
  pmOptions(new Some(42)).tap(println) // new Some(42) != Some(42))

  // generics
  pmGeneric(Some(43)).tap(println)
  pmGeneric(None).tap(println)

  // any
  pmAny(Some("Scala")).tap(println)
  pmAny(Some(42)).tap(println)
  pmAny(Some(42.4)).tap(println)

  pmErasureAvoidance(List(1, 2, 3)).tap(println)
  pmErasureAvoidance(List("1", "2", "3")).tap(println)
  pmErasureAvoidance(List('a', 'b', 'c')).tap(println)

  pmListExpression(List(1, 2, 3).map(_.toString).map(_.length)).tap(println)
  pmListExpression(List("a").map(_.length).map(_.toString)).tap(println)
