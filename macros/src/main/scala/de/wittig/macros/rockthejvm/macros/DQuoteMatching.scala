package de.wittig.macros.rockthejvm.macros
import quoted.*

object DQuoteMatching:

  inline def pmOptions(inline opt: Option[Int]): String = ${ pmOptionsImpl('opt) }

  private def pmOptionsImpl(opt: Expr[Option[Int]])(using Quotes): Expr[String] =
    val result = opt match
      case '{ Some(42) } => "Got the meaning of life"
      case '{ Some($x) } => "Got some other value: " + x.show
      case _             => "Unknown"
    Expr(result)

  inline def pmGeneric[T](inline x: Option[T]): String = ${ pmGenericImpl('x) }

  private def pmGenericImpl[T: Type](x: Expr[Option[T]])(using Quotes): Expr[String] =
    val result = x match
      case '{ Some($x) } => "Got some other value: " + x.show + " of type" + Type.show[T]
      case _             => "Unknown"
    Expr(result)

  inline def pmAny(inline x: Option[Any]): String = ${ pmAnyImpl('x) }

  private def pmAnyImpl(x: Expr[Option[Any]])(using Quotes): Expr[String] =
    val result = x match
      case '{ Some($x: String) } => "Got a string: " + x.show
      case '{ Some($x: Int) }    => "Got an int: " + x.show
      case '{ Some($x) }         => "Got some other type: " + x.show
      case _                     => "Unknown"
    Expr(result)

  inline def pmErasureAvoidance(inline x: List[Any]): String = ${ pmErasureAvoidanceImpl('x) }

  private def pmErasureAvoidanceImpl(x: Expr[List[Any]])(using Quotes): Expr[String] =
    val result = x match
      case '{ $l: List[Int] } => "Got a list of ints"
      case '{ $l: List[t] }   => "Got a list of " + Type.show[t] // we magically get a given Type[t] when matchen in a pattern
      case _                  => "Got a list of something else"
    Expr(result)

  inline def pmListExpression(inline x: List[Any]): String = ${ pmListExpressionImpl('x) }

  private def pmListExpressionImpl(x: Expr[List[Any]])(using Quotes): Expr[String] =
    val result = x match
      case '{
            type t1 // TYPE VARIABLE, not an abstract type // Narrowing type t1 <: AnyVal is possible
            ($l: List[`t1`]).map[t2]($f).map[`t1`]($g)
          } => "Got a chain of list maps between types " + Type.show[t1] + " and " + Type.show[t2]
      case _ => "Got something else"
    Expr(result)
