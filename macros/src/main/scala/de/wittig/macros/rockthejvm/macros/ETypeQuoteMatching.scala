package de.wittig.macros.rockthejvm.macros
import quoted.*
import scala.util.Try

object ETypeQuoteMatching:

  inline def matchType[A]: String = ${ matchTypeImpl[A] }

  private def matchTypeImpl[A: Type](using Quotes): Expr[String] =
    val tpe    = Type.of[A]
    val result = tpe match
      case '[Int]          => "It's an Int!"
      case '[String]       => "It's a String!"
      case '[List[t]]      => s"It's a List of ${Type.show[t]}"
      case '[Either[a, b]] => s"Either of ${Type.show[a]} or ${Type.show[b]}"
      case '[a => b]       => s"Function type from ${Type.show[a]} to ${Type.show[b]}"

      // can have type restrictions
      case '[type a; (`a`, b, `a`)] => s"Tuple with 3 type members. first/third are the same: 1/3:${Type.show[a]} and 2:${Type.show[b]}"

      case '[type a <: AnyVal; Try[`a`]] => s"Try of a subtype of AnyVal: ${Type.show[a]}"
      case _                             => "Unknown type"
    Expr(result)
