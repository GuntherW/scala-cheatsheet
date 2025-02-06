package de.wittig.macros.rockthejvm.macrosUsage
import de.wittig.macros.rockthejvm.macros.AMacroIntro.firstMacro
import scala.util.chaining.scalaUtilChainingOps
import de.wittig.macros.rockthejvm.macros.AMacroIntro.firstMacroInlineArguments

object AMacroIntro extends App:

  firstMacro(3, "Scala").tap(println)
  firstMacro(2 + 1, "Scala").tap(println)

  val number = 3
  val string = "Scala"
  // val result2 = firstMacro(number, string) // error, because the values are not compile time constants

  val inlineExpanded = firstMacroInlineArguments(2 + number, "Scala".repeat(3))
  println(inlineExpanded)
