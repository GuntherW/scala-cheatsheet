package de.wittig.macros.baeldung

import scala.quoted.{Expr, Quotes}

/** https://www.baeldung.com/scala/macros-scala-3
  */
object Inliners:

  // Inlining garantiert zur Kompilierzeit.
  inline def oddEvenInline(number: Int): String =
    number % 2 match
      case 0 => "even"
      case _ => "odd"

  inline def oddEvenInlineArgument(inline number: Int): String =
    number % 2 match
      case 0 => "even"
      case _ => "odd"

  inline def oddEvenInlineInlineMatch(inline number: Int): String =
    inline number % 2 match
      case 0 => s"even 0 $number"
      case s => s"odd $s $number"

  // transparent for refining return type of Method
  transparent inline def oddEvenTransparent(inline number: Int): "even" | "odd" =
    inline number % 2 match
      case 0 => "even"
      case s => "odd"
