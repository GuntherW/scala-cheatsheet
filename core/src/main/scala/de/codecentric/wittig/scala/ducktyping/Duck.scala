package de.codecentric.wittig.scala.ducktyping
import reflect.Selectable.reflectiveSelectable
import scala.language.reflectiveCalls

object Duck extends App:

  def quaken(duck: { def quak(s: String): String }): Unit =
    println(duck.quak("Quak"))

  object BigDuck:
    def quak(s: String): String = s.toUpperCase

  object SmallDuck:
    def quak(s: String): String = s.toLowerCase

  object AnythingButADuck:
    def quak(s: String): String = s"I am different: $s"

  quaken(BigDuck)
  quaken(SmallDuck)
  quaken(AnythingButADuck)
