package de.codecentric.wittig.scala

import scala.Console.*

object Printer {

  def printlnYellow(msg: String): Unit  = printlnColored(YELLOW, YELLOW_B)(msg)
  def printlnGreen(msg: String): Unit   = printlnColored(GREEN, GREEN_B)(msg)
  def printlnBlue(msg: String): Unit    = printlnColored(BLUE, BLUE_B)(msg)
  def printlnRed(msg: String): Unit     = printlnColored(RED, RED_B)(msg)
  def printlnCyan(msg: String): Unit    = printlnColored(CYAN, CYAN_B)(msg)
  def printlnMagenta(msg: String): Unit = printlnColored(MAGENTA, MAGENTA_B)(msg)

  private def printlnColored(c: String, cb: String)(s: String): Unit = {
    val hyphenStart = s"$c${"-" * 20}$RESET"
    val hyphenEnd   = s"$c${("-" * 40).take(40 - s.length)}$RESET"
    val msg         = s"$cb $s $RESET"

    println(s"$hyphenStart$msg$hyphenEnd")
  }
}
