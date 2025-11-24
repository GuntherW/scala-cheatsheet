package de.wittig.macros.rockthejvm.macros

import quoted.*

object IExprList:
  inline def processVarargs(inline values: Int*): String =
    ${ processVarargsImpl('values) }

  private def processVarargsImpl(values: Expr[Seq[Int]])(using Quotes): Expr[String] =

    values match
      case Varargs(elements) =>
        val num     = elements.map(_.show)
        val numExpr = Expr(num.mkString(", "))
        '{ "got these numbers: " + $numExpr }
      case _                 => Expr("got something else")

  inline def returnExpr = ${ returnExprImpl }

  private def returnExprImpl(using Quotes): Expr[List[String]] =

    val expr: List[Expr[String]] = List('{ "Hello" * 2 }, '{ "World".toUpperCase })

    // can turn a List[Expr[String]] into an Expr[List[String]]
    val finalExpr: Expr[List[String]] = Expr.ofList(expr)
    finalExpr
