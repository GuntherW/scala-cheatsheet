package de.wittig.macros.rockthejvm.macrosUsage
import de.wittig.macros.rockthejvm.macros.DQuoteMatching.*

object DQuoteMatching extends App:
  val a = pmOptions(Some(42))
  val b = pmOptions(Some(43))
  val c = pmOptions(Option(42))   // Option(42) != Some(42))
  val d = pmOptions(new Some(42)) // new Some(42) != Some(42))

  // generics
  pmGeneric(Some(43))
  pmGeneric(None)

  // any
  pmAny(Some("Scala"))
  pmAny(Some(42))
  pmAny(Some(42.4))

  pmErasureAvoidance(List(1, 2, 3))
  pmErasureAvoidance(List("1", "2", "3"))
  pmErasureAvoidance(List('a', 'b', 'c'))

  pmListExpression(List(1, 2, 3).map(_.toString).map(_.length))
  pmListExpression(List("a").map(_.length).map(_.toString))
