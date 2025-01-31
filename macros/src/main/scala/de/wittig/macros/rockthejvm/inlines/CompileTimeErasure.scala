package de.wittig.macros.rockthejvm.inlines

object CompileTimeErasure extends App:

  import compiletime.erasedValue
  import compiletime.constValue

  inline def pmOnType[A] =
    inline erasedValue[A] match
      case _: String => "This is a String"
      case _: Int    => "This is an Int"
      case _         => "This is something else"

  val messageString = pmOnType[String]
  val messageInt    = pmOnType[Int]
  val messageSmth   = pmOnType[Boolean]

  import compiletime.ops.int.S // Successor
  import compiletime.ops.int.+

  transparent inline def factorial[N <: Int]: Int =
    inline erasedValue[N] match
      case _: 0    => 1
      case _: S[n] => constValue[n + 1] * factorial[n]

  val fact4: 24 = factorial[4]
