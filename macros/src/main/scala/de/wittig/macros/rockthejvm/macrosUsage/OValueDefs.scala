package de.wittig.macros.rockthejvm.macrosUsage
import de.wittig.macros.rockthejvm.macros.OValueDefs.*

object OValueDefs extends App:

  // synthesized:
  // scalaLength = {
  //    val myValue = "Scala".length
  //    myValue * 4
  // }
  val scalaLength = buildValueDef
