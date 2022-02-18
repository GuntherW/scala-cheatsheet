package de.codecentric.wittig.scala

import scala.Console.*

object Printer:

  def printlnYellow(msg: Any): Unit  = printlnColored(YELLOW, YELLOW_B)(msg)
  def printlnGreen(msg: Any): Unit   = printlnColored(GREEN, GREEN_B)(msg)
  def printlnBlue(msg: Any): Unit    = printlnColored(BLUE, BLUE_B)(msg)
  def printlnRed(msg: Any): Unit     = printlnColored(RED, RED_B)(msg)
  def printlnCyan(msg: Any): Unit    = printlnColored(CYAN, CYAN_B)(msg)
  def printlnMagenta(msg: Any): Unit = printlnColored(MAGENTA, MAGENTA_B)(msg)

  private def printlnColored(c: String, cb: String)(s: Any): Unit =
    val hyphenStart = s"$c${"-" * 20}$RESET"
    val hyphenEnd   = s"$c${("-" * 40).take(40 - s.toString.length)}$RESET"
    val msg         = s"$cb ${s.toString} $RESET"

    println(s"$hyphenStart$msg$hyphenEnd")
