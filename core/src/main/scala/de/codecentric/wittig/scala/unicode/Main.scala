package de.codecentric.wittig.scala.unicode

object Main extends App:

  private val nbsp    = "\u00a0"
  private val lambda  = "\u03bb" // right click -> "String Manipulation" -> "Escape/Unescape" -> "Escape Unicode to String"
  private val lambda2 = "λ"      // Mit "String Manipulation" Plugin, oder mit KRunner: #03bb
  println(s"$lambda$nbsp$lambda2")
