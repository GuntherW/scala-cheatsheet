package de.codecentric.wittig.scala

import scala.Console._
object Printer {

  def log(s: String): Unit = {
    val hyphenStart = s"$YELLOW${"-" * 20}$RESET"
    val hyphenEnd   = s"$YELLOW${("-" * 40).take(40 - s.length)}$RESET"
    val msg         = s"$YELLOW_B $s $RESET"

    println("")
    println(s"$hyphenStart$msg$hyphenEnd")
  }
}
