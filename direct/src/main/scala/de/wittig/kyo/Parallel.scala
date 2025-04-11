package de.wittig.kyo

import kyo.*

object Parallel extends KyoApp {

  private val programm = defer {

    // An example computation
    val a: Int =
      IO(Math.cos(42).toInt).now

    // There are method overloadings for up to four
    // parallel computations. Parameters taken by
    // reference
    val b: (Int, String) = Async.zip(a, "example").now
    println(b)
  }

  run(programm)
}
