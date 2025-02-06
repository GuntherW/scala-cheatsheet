package de.wittig.macros.rockthejvm.macrosUsage

import de.wittig.macros.rockthejvm.macros.IExprList.*

object IExprList extends App:
  val varargsDescriptor = processVarargs(1 * 2, 2, 3)
  val listOfExprs       = returnExpr

  println(listOfExprs) // List(HelloHello, WORLD)
