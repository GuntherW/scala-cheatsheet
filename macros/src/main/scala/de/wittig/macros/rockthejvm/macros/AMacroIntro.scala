package de.wittig.macros.rockthejvm.macros

import scala.quoted.Expr
import scala.quoted.Quotes

object AMacroIntro:

  inline def firstMacro(number: Int, string: String): String =
    ${ firstMacroImpl('number, 'string) }

  // macro implementation, manipulating ASTs at compile time
  def firstMacroImpl(numAST: Expr[Int], stringAST: Expr[String])(using Quotes): Expr[String] =
    // Expr[A] can be turned into a value if it's known at compile time
    val numValue    = numAST.valueOrAbort
    val stringValue = stringAST.valueOrAbort

    // expressions can be evaluated at compile time
    val newString =
      if (stringValue.length > 10) stringValue.take(numValue)
      else stringValue.repeat(numValue)

    Expr("This macro impl is: " + newString)

  inline def firstMacroInlineArguments(inline number: Int, inline string: String): String =
    ${ firstMacroInlineArgumentsImpl('number, 'string) }

  def firstMacroInlineArgumentsImpl(numAST: Expr[Int], stringAST: Expr[String])(using Quotes): Expr[String] =
    Expr("The number: " + numAST.show + ", the string: " + stringAST.show)
