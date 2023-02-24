package de.wittig.metaprogramming.inline

inline def sayHallo(s: String): Unit = // definition site
  println(s"Hallo $s")

/** Im Gegensatz zu Scala2s @inline ist das "inline" in Scala3 keine "Suggestion" sondern wird garantiert geinlined.
  */
object Simple extends App:
  sayHallo("Test") // call site
