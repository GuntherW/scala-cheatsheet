package de.wittig.macros.staging

import scala.quoted.*
import scala.quoted.staging.Compiler

/** Staging demo: generates an unrolled power function at *runtime* of the program.
  *
  * Run with: sbt --client "macros/runMain de.wittig.macros.staging.runUnrolledPowerDemo 4"
  */
object UnrolledPowerDemo:

  // Needed to run/show quoted code at runtime. Creates a Scala compiler instance (expensive, reuse it!).
  given Compiler = Compiler.make(getClass.getClassLoader)

  /** Generates `x => x * x * ... * x` (n times) with the multiplications unrolled. n is only known at runtime. */
  def unrolledPower(n: Int)(using Quotes): Expr[Double => Double] =
    def go(x: Expr[Double], n: Int): Expr[Double] =
      if n <= 0 then Expr(1.0)
      else if n == 1 then x
      else '{ $x * ${ go(x, n - 1) } }
    '{ (x: Double) => ${ go('x, n) } }

  def run(args: List[String]): Unit =
    val exponent = args.headOption.map(_.toInt).getOrElse(5) // value known only at runtime

    // withQuotes + show: pretty prints the generated code
    println(s"generated code: ${staging.withQuotes(unrolledPower(exponent).show)}")

    // run: compiles the code in-process and returns the result
    val power: Double => Double = staging.run(unrolledPower(exponent))
    println(s"2.0^$exponent = ${power(2.0)}")

@main
def runUnrolledPowerDemo(args: String*): Unit = UnrolledPowerDemo.run(args.toList)
