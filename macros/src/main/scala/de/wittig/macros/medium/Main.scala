package de.wittig.macros.medium
import scala.compiletime.summonInline

object Inlines {
  given context: String = "Context:Inlines"

  inline def f(inline p: Int): Unit =
    val ctx = summonInline[String]
    println(s"f1   -> p: $p, ctx: $ctx")
    println(s"f2   -> p: $p, ctx: $ctx")

  def g(p: Int): Unit =
    val ctx = summonInline[String]
    println(s"g1   -> p: $p, ctx: $ctx")
    println(s"g2   -> p: $p, ctx: $ctx")
}

// https://medium.com/codex/scala-3-macros-without-pain-ce54d116880a
object Main extends App {
  import Inlines.*

  given context: String = "Context:Main"

  private var a = 1

  private def sideEffect(): Int =
    a += 1
    a

  f(sideEffect())
  g(sideEffect())
}
