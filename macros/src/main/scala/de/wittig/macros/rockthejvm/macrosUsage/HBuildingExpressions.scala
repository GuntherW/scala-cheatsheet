package de.wittig.macros.rockthejvm.macrosUsage
import de.wittig.macros.rockthejvm.macros.HBuildingExpressions.*

object HBuildingExpressions extends App:

  val a = createDefaultPermissions
  println(a)

  val b = describePermissions(Permissions.Custom(List("a", "b", "c")))
  println(b)
