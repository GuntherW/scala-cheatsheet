package de.wittig.kyo

import kyo.*

object Main extends KyoApp {

  val programm = defer {

    // Effectful value
    val b: Int = IO(10_000).now
    println(b)

    // Control flow
    val c: String = if IO(true).now then "True branch" else "False branch"

    // Logical operations
    val d: Boolean = IO(true).now && IO(false).now

    val e: Boolean = IO(true).now || IO(true).now

    // Loop (for demonstration; this loop
    // won't execute its body)
    while (IO(false).now)
      println("looping")

    // Pattern matching
    val matchResult: String =
      IO(1).now match
        case 1 => "One"
        case _ => "Other"

    println(matchResult)
  }

  run(programm)
}
