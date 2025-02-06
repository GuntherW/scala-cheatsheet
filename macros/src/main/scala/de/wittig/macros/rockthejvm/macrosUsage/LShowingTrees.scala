package de.wittig.macros.rockthejvm.macrosUsage
import de.wittig.macros.rockthejvm.macros.LShowingTrees.*

object LShowingTrees extends App:

  debugExpr(List(1, 2).map(_.toString))
