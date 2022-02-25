package de.wittig.scala.scalacheck.helper

object Printer:
  def printn(a: String, b: String): Unit =
    println(s"arb a: [$a]")
    println(s"arb b: [$b]")
    println("\u2500" * 50)
