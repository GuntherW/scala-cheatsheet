package de.wittig.macros.rockthejvm.macrosUsage
import de.wittig.macros.rockthejvm.macros.FErrorReporting.*

@main
def fErrorReporting(): Unit =
  val someIntExpression = functionWithErrors(42 + 2)
  // val errorIntExpression = functionWithErrors(-2) // will not compile.

  val aVariable = 2
  // val notCompile2 = functionWithErrors(aVariable) // will not compile, because we use a variable

  // val lessThan0  = functionWithErrorsNoAbort(-5) // will not compile
  // val lessThan10 = functionWithErrorsNoAbort(5)  // will not compile

  // val accumError = errorReporting2(-5, -5) // will not compile and report both errors
