package de.wittig.macros.playground

import de.wittig.macros.playground.Timed.timed

@main
def main(): Unit =
  def myFunction(n: Int): Int =
    Thread.sleep(100 * n)
    20

  timed(myFunction(4))
  val i = 6
  timed(myFunction(i))
