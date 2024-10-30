package de.wittig.kyo

import kyo.*

object Main extends KyoApp {

  val programm = defer {

    // Effectful value
    val b: Int = await(IO(10_000))
    println(b)

    // Control flow
    val c: String = if await(IO(true)) then "True branch" else "False branch"

    // Logical operations
    val d: Boolean = await(IO(true)) && await(IO(false))

    val e: Boolean = await(IO(true)) || await(IO(true))

    // Loop (for demonstration; this loop
    // won't execute its body)
    while (await(IO(false)))
      println("looping")

    // Pattern matching
    val matchResult: String =
      await(IO(1)) match
        case 1 => "One"
        case _ => "Other"

    println(matchResult)
  }

  run(programm)
}
