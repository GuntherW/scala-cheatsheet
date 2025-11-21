package de.codecentric.wittig.scala.dynamic.selectabled

@main
def main(): Unit =

  type RefinedResult = DynS { val a: String; val b: Int; val c: String }
  val m       = Map("a" -> "Gunther", "b" -> 99)
  val rResult = DynS(m).asInstanceOf[RefinedResult]

  println(rResult.a)
  println(rResult.b)
  println(rResult.c)

case class DynS(map: Map[String, Any]) extends Selectable:
  def selectDynamic(name: String): Any = map.getOrElse(name, "nix")
