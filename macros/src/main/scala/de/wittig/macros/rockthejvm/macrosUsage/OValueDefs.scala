package de.wittig.macros.rockthejvm.macrosUsage
import de.wittig.macros.rockthejvm.macros.OValueDefs.*

@main
def oValueDefs(): Unit =

  // synthesized:
  // scalaLength = {
  //    val myValue = "Scala".length
  //    myValue * 4
  // }
  val scalaLength = buildValueDef
