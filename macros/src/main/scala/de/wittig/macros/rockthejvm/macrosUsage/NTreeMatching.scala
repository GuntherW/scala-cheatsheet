package de.wittig.macros.rockthejvm.macrosUsage
import de.wittig.macros.rockthejvm.macros.NTreeMatching.*

object NTreeMatching extends App:
  def multiply(x: Int, y: Int): Int = x * y

  demoTreeMatching(multiply(2, 3))
