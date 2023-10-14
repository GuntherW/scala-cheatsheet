package de.wittig.macros.langton

import scala.collection.immutable.List
import scala.quoted.*

object internal:
  opaque type Into[From, To] = From
import internal.Into

extension [From](self: From)
  def into[To]: Into[From, To] = throw new Error("into should never be called at runtime")

extension [From, To](inline self: Into[From, To])
  // 1. macro definition
  inline def transform: To = ${ transformImpl[From, To]('self) }

// 2. macro implementation
def transformImpl[From: Type, To: Type](expr: Expr[Into[From, To]])(using Quotes): Expr[To] =
  import quotes.reflect.*

  expr match
    case '{ de.wittig.macros.langton.into[From]($fromExpr)[To] } =>
      val fromFields    = getFields[From]
      val fromFieldsMap = fromFields.map(f => f.name -> f).toMap
      val toFields      = getFields[To]

      val projections = toFields.map { toField =>
        val field = fromFieldsMap(toField.name)
        projectField(fromExpr, field)
      }

      construct[To](projections)
    case _                                                       =>
      report.errorAndAbort(s"EXPR: ${expr.show}")

def getFields[A: Type](using Quotes): List[quotes.reflect.Symbol] =
  import quotes.reflect.*
  TypeRepr.of[A].typeSymbol.caseFields

def projectField[A: Type](using Quotes)(
    expr: Expr[A],
    fieldSymbol: quotes.reflect.Symbol
): Expr[Any] =
  import quotes.reflect.*
  Select(expr.asTerm, fieldSymbol).asExpr

def construct[A: Type](args: List[Expr[Any]])(using Quotes): Expr[A] =
  import quotes.reflect.*
  val companionSymbol = TypeRepr.of[A].typeSymbol.companionModule
  val result          = Apply(
    fun = Select.unique(Ref(companionSymbol), "apply"),
    args = args.map(_.asTerm)
  ).asExprOf[A]
  result
