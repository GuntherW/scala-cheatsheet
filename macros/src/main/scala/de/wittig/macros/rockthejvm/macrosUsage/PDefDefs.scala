package de.wittig.macros.rockthejvm.macrosUsage

import de.wittig.macros.rockthejvm.macros.PDefDefs.*

@main
def pdefDefs(): Unit =
  val dynamicFunctionApplication = generateDynamicFunction(3, "Scala", true)
  println(dynamicFunctionApplication)
