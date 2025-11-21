package de.codecentric.wittig.scala.unicode

@main
def main(): Unit =

  val nbsp    = "\u00a0"
  val lambda  = "\u03bb" // right click -> "String Manipulation" -> "Escape/Unescape" -> "Escape Unicode to String"
  val lambda2 = "λ"      // Mit "String Manipulation" Plugin, oder mit KRunner: #03bb
  println(s"$lambda$nbsp$lambda2")
