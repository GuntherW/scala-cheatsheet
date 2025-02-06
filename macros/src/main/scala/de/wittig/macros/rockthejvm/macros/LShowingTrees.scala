package de.wittig.macros.rockthejvm.macros
import quoted.*

object LShowingTrees:

  inline def debugExpr[A](inline value: A) = ${ debugExprImpl('value) }

  private def debugExprImpl[A: Type](value: Expr[A])(using Quotes): Expr[Unit] =
    import quotes.reflect.*

    val term     = value.asTerm
    val typeRepr = TypeRepr.of[A]

    println("################## start ###################")
    println(term.show) // with fully qualified names
    println(term.show(using Printer.TreeShortCode))
    println(term.show(using Printer.TreeStructure))
    println("################## ----- ###################")
    println(typeRepr.show)
    println(typeRepr.show(using Printer.TypeReprShortCode))
    println(typeRepr.show(using Printer.TypeReprStructure))
    println("################## end #####################")
    '{ () }
