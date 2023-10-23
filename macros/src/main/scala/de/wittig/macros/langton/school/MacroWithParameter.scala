package de.wittig.macros.langton.school

import scala.quoted.*

object MacroWithParameter {

  inline def length(str: String): Int = ${ lengthImpl('str) }

  def lengthImpl(str: Expr[String])(using Quotes): Expr[Int] = '{ $str.length() }
}
