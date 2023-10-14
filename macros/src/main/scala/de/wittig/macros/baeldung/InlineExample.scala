package de.wittig.macros.baeldung

import scala.quoted.{Expr, Quotes}

/** https://www.baeldung.com/scala/macros-scala-3
  */
object InlineExample:

  inline def oddEvenMacroInline(inline number: Int): String =
    number % 2 match
      case 0 => "even"
      case _ => "odd"

  inline def oddEvenMacroInlineConditional(inline number: Int): String =
    inline number % 2 match
      case 0 => "even"
      case s => "odd"

  // transparent for refining return type of Method
  transparent inline def oddEvenMacroTransparent(inline number: Int): "even" | "odd" =
    inline number % 2 match
      case 0 => "even"
      case _ => "odd"
