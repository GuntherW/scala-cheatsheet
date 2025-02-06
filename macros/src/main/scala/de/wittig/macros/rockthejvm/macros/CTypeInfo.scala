package de.wittig.macros.rockthejvm.macros

import scala.quoted.*

object CTypeInfo:

  inline def myLittleMacrox(x: Int): Int = ${ myLittleMacroImpl('x) }

  private def myLittleMacroImpl(expr: Expr[Int])(using Quotes): Expr[Int] =

    // instance describing a Type. Like a TypeTag in regular code. Only available inside a macro implementation.
    val intType: Type[Int] = Type.of[Int]

    // This type info is available BEFORE type erasure
    val listIntTypeDescription: String = Type.show[List[Int]]

    Expr(42) // not important
