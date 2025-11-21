package de.wittig.macros.rockthejvm.inlines
import scala.util.chaining.scalaUtilChainingOps

@main
def fCompileTimeErasure(): Unit =

  import compiletime.erasedValue
  import compiletime.constValue

  inline def patternMatchOnType[A] =
    inline erasedValue[A] match // erased value returns a fictive literal value on the type A, I can match on.
      case _: String => "This is a String"
      case _: Int    => "This is an Int"
      case _         => "This is something else"

  val messageString = patternMatchOnType[String].tap(println)
  val messageInt    = patternMatchOnType[Int].tap(println)
  val messageSmth   = patternMatchOnType[Boolean].tap(println)

  import compiletime.ops.int.S // Successor
  import compiletime.ops.int.+

  transparent inline def factorial[N <: Int]: Int =
    inline erasedValue[N] match
      case _: 0    => 1
      case _: S[n] => constValue[n + 1] * factorial[n]

  val fact4: 24 = factorial[4].tap(println)
