package de.wittig.macros.rockthejvm.macros

import scala.quoted.*

object OValueDefs:

  inline def buildValueDef = ${ buildValueDefImpl }

  // val x:Int = "Scala".length
  private def buildValueDefImpl(using Quotes): Expr[Int] =
    import quotes.reflect.*

    val mySymbol = Symbol.newVal(
      parent = Symbol.spliceOwner,
      name = "myValue",       // name of the new val
      tpe = TypeRepr.of[Int], // type of the new val
      flags = Flags.Lazy,
      privateWithin = Symbol.noSymbol
    )

    val valBody =
      // technical detail: the given Quotes needs to be given by the symbol
      given Quotes = mySymbol.asQuotes
      '{ "Scala".length }

    val valueDef = ValDef(
      symbol = mySymbol,
      rhs = Some(valBody.asTerm)
    )

    // myValue * 4 => refer to the value def
    val valRef = Ref(mySymbol).asExprOf[Int]

    // expression myValue * 4
    val finalExpr = '{ $valRef * 4 }

    Block(
      stats = List(valueDef),
      expr = finalExpr.asTerm
    ).asExprOf[Int]
