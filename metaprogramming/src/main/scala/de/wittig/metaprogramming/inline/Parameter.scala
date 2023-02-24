package de.wittig.metaprogramming.inline

import scala.util.Random

inline def printTwiceOhne(x: Int): Unit =
  println(x)
  println(x)

inline def printTwice(inline x: Int): Unit =
  println(x)
  println(x)

// Will print two different random Ints, because code is inlined and duplicated
object Parameter extends App:
  private val rand = new Random(0)
  printTwiceOhne(rand.nextInt())
  println("-"*11)
  printTwice(rand.nextInt())
