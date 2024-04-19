package de.wittig.macros.medium
import scala.compiletime.summonInline
import Inlines.*

object Inlines {
  given context: String = "Context:Inlines"

  def f(p: Int): Unit =
    val ctx = summonInline[String]
    println(s"f              -> p: $p, ctx: $ctx")
    println(s"f              -> p: $p, ctx: $ctx")

  inline def fInline(p: Int): Unit =
    val ctx = summonInline[String]
    println(s"fInline        -> p: $p, ctx: $ctx")
    println(s"fInline        -> p: $p, ctx: $ctx")

  inline def fInlineLazy(p: => Int): Unit =
    val ctx = summonInline[String]
    println(s"fInlineLazy    -> p: $p, ctx: $ctx")
    println(s"fInlineLazy    -> p: $p, ctx: $ctx")

  inline def fInlineInline(inline p: Int): Unit =
    val ctx = summonInline[String]
    println(s"fInlineInline  -> p: $p, ctx: $ctx")
    println(s"fInlineInline  -> p: $p, ctx: $ctx")

}

// https://medium.com/codex/scala-3-macros-without-pain-ce54d116880a
object Main extends App {

  given context: String = "Context:Main"

  private var a = 0

  private def sideEffect(): Int =
    a += 1
    a

  f(sideEffect())
  a = 0
  fInline(sideEffect())
  a = 0
  fInlineLazy(sideEffect())
  a = 0
  fInlineInline(sideEffect())
}
