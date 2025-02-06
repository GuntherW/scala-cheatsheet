package de.codecentric.wittig.scala.dynamic.selectabled

object Main extends App:

  private type RefinedResult = DynS { val a: String; val b: Int; val c: String }
  private val m       = Map("a" -> "Gunther", "b" -> 99)
  private val rResult = DynS(m).asInstanceOf[RefinedResult]

  println(rResult.a)
  println(rResult.b)
  println(rResult.c)

case class DynS(map: Map[String, Any]) extends Selectable:
  def selectDynamic(name: String): Any = map.getOrElse(name, "nix")
