package de.wittig.scala.scalacheck.helper

object Printer:
  def printn(s: String*): Unit =
    s.zipWithIndex.foreach {
      case (s, i) => println(s"$i: [$s]")
    }
    println("\u2500" * 50)
