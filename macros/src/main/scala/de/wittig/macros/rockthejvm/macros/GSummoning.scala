package de.wittig.macros.rockthejvm.macros

import scala.quoted.*

object GSummoning:

  trait MyTypeClass[A]:
    def message: String

  inline def describeType[A <: Tuple]: String = ${ describeTypeImpl[A] }

  private def describeTypeImpFaultyl[A <: Tuple: Type](using Quotes): Expr[String] =
    Type.of[A] match
      case '[(_, a, _)] =>
        // we can't access a give MyTypeClass[a] because a is a type variable
        // '{ summon[MyTypeClass[a]].message } // This doesn't work
        Expr("can't summon MyTypeClass[a] because a is a type variable")
      case _            => Expr("some type I don't know about")

  // Expr.summon - delays the summoning to the macro expansion site
  private def describeTypeImpl[A <: Tuple: Type](using q: Quotes): Expr[String] =
    import q.reflect.*
    Type.of[A] match
      case '[(_, a, _)] =>
        val maybeTypeClass = Expr.summon[MyTypeClass[a]] // delay of summoning, returns an Option[Expr[MyTypeClass[a]]]
        val typeClass      = maybeTypeClass.getOrElse(report.errorAndAbort(s"Missing type class for ${Type.show[a]}"))
        '{ $typeClass.message }
      case _            => Expr("some type I don't know about")
