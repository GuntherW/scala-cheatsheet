package de.wittig.macros.baeldung

import scala.quoted.{Expr, Quotes}

/** https://www.baeldung.com/scala/macros-scala-3
  */
object MacroExample:

  /** Quouting
    *
    * Quoting is the process of casting Scala code into an Expr. It lets us create simpler code that we can use for our macros.
    */
  def oddEvenQuotes(n: Expr[Int])(using Quotes): Expr[String] = '{
    $n % 2 match
      case 0 => "even"
      case _ => "odd"
  }

  /** Splicing
    *
    * From Expr to ScalaCode
    */
  inline def oddEvenMacroQuote(inline number: Int): String = ${ oddEvenQuotes('number) }
