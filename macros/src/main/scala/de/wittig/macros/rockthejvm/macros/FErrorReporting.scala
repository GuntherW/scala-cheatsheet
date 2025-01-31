package de.wittig.macros.rockthejvm.macros
import quoted.*

object FErrorReporting:

  inline def functionWithErrors(x: Int): Int = ${ functionWithErrorsImpl('x) }

  private def functionWithErrorsImpl(x: Expr[Int])(using q: Quotes): Expr[Int] =
    import q.reflect.* // needed to run error reports

    if x.valueOrAbort < 0 then
      report.errorAndAbort(s"The number ${x.show} is less then 0")
    '{ $x + 3 }

  inline def functionWithErrorsNoAbort(x: Int): Int = ${ functionWithErrorsNoAbortImpl('x) }

  private def functionWithErrorsNoAbortImpl(x: Expr[Int])(using q: Quotes): Expr[Int] =
    import q.reflect.* // needed to run error reports

    val value = x.valueOrAbort

    // errors will not be accumulated - the compiler will stop at the first one
    if value < 0 then
      report.error(s"The number ${x.show} is less then 0")

    if value < 10 then
      report.error(s"The number ${x.show} is less then 10")
    '{ $x + 3 }

  inline def errorReporting2(x: Int, y: Int): Int = ${ errorReporting2Impl('x, 'y) }

  private def errorReporting2Impl(x: Expr[Int], y: Expr[Int])(using q: Quotes): Expr[Int] =
    import q.reflect.* // needed to run error reports

    val xValue = x.valueOrAbort
    val yValue = y.valueOrAbort

    if xValue < 0 then
      report.error(s"The first expression ${x.show} is less then 0", x)

    if yValue < 0 then
      report.error(s"The second expression ${y.show} is less then 0", y)

    '{ $x + $y }
