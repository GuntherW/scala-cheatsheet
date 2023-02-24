package de.wittig.metaprogramming.timed

import de.wittig.metaprogramming.timed.Timed.timed

object Main extends App:
  private def myFunction(n: Int): Int =
    Thread.sleep(100 * n)
    20

  timed(myFunction(4))
  private val i = 6
  timed(myFunction(i))
