package de.wittig.macros.rockthejvm.macros
import quoted.*

object BQuoting {

  inline def runPlayground(string: String): String = ${ macroPlayground('string) }

  def macroPlayground(stringExpr: Expr[String])(using Quotes): Expr[String] =

    val anExpr          = '{ "some constant string" }
    val moreComplexExpr = '{ "some constant string" + $stringExpr }

    // casting Expr
    val anyExpr: Expr[Any] = '{ "some constant string" }
    val recoveredExpr      = anyExpr.asExprOf[String] // .asExprOf[T] is a cast like .asInstanceOf[T] in regular code.
    val aQoutedExpr        = '{ $recoveredExpr.drop(1) + "!" }

    // Level
    val aSimpleString        = "some string".repeat(2)        // "level" 0
    // val illegalUsageOfVariable = '{            // quouting starts a new "level"
    //  "Hallo " + $aSimpleString // illegal, because a variable, defined at another level, can not be provably expanded
    // }
    val legalUsageOfVariable = Expr("Hallo " + aSimpleString) // legal, because the variable is used at the same level

    // Correct way
    val aSimpleExpr      = '{ "some string".repeat(2) } // Expr[String]
    val anExprWithString = '{
      "Hallo " + $aSimpleExpr // legal
    }

    legalUsageOfVariable

}
