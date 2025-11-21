package de.wittig.macros.rockthejvm.macrosUsage
import de.wittig.macros.rockthejvm.macros.LShowingTrees.*

@main
def lShowingTrees(): Unit =

  debugExpr(List(1, 2).map(_.toString))
