package de.wittig.macros.langton.simple

import scala.quoted.*

object DebugMacro {

  // 1. macro definition
  //  + always need to be inlined
  //  + body needs to have exactly one "splice"
  inline def debug(value: String): String = ${ debugImpl('value) }

  // 2. macro implementation (Expr => Expr)
  //  + needs the "import quotes.reflect.*"
  private def debugImpl(value: Expr[String])(using Quotes): Expr[String] =
    import quotes.reflect.*

//    report.errorAndAbort(s"Was ist das? ${value.asTerm.underlyingArgument}")
    value.asTerm.underlyingArgument match
      case Ident(name) => '{ ${ Expr(name) } + "=" + $value }
      case _           => value

  // 3. You can not call the macro in the same file.
}
