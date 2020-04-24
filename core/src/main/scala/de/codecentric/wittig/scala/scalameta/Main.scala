package de.codecentric.wittig.scala.scalameta

import scala.meta._

object Main extends App {

  println(c("Hallo Welt"))
  val program = """object Main extends App { print("Hello!") }"""
  val tree    = program.parse[Source].get
  println(c(tree.syntax))

  def c(s: String): String = Console.CYAN + s + Console.RESET
}
