package de.codecentric.wittig.scala.stringcontext

import java.time.LocalDate

@main
def fInterpolator(): Unit =

  val a = 127
  val b = "zwei"
  val c = 12345.234
  val d = LocalDate.now
  val e = true

  println(s"$a,$b,$c,$d,$e")
  println(f"$a,$b,$c,$d,$e")
  println(f"${"hallo"}%-10s|${"welt"}%-10s|")         // Überschriften
  println(f"$a%-20s|$b%-10s|$c%-10s|$d%-10s|$e%-10b") // s String // S Uppercase String
  println(f"$a%-20S|$b%-10S|$c%-10S|$d%-10S|$e%-10S")
  println(f"$a%-20S|$b%-10S|$c%-10S|$d%-10S|$e%-10B")
  println(f"$a%h|$b%h|$c%h|$d%h|$e%h")                // h = Hashcode
  println(f"$a%H|$b%H|$c%H|$d%H|$e%H")                // H = Uppercase Hashcode
  println(f"$c%#.2f")                                 // # Alternate output format, Nachkommastellen
  println(f"$a%#.2f")                                 // # Alternate output format, Nachkommastellen
  println(f"$a%#x")                                   // # Alternate output format, In Hexadezimalschreibweise
  println(f"$c%.3e")                                  // # Alternate output format, In Oktalschreibweise
