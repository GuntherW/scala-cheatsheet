package de.wittig.macros.rockthejvm.macrosUsage

import de.wittig.macros.rockthejvm.macros.PDefDefs.*

object PDefDefs extends App {
  val dynamicFunctionApplication = generateDynamicFunction(3, "Scala", true)
  println(dynamicFunctionApplication)
}
