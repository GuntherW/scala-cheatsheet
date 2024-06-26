package de.wittig.kyo

import kyo.*

object Main extends App {

  val programm = defer {

    // Effectful value
    val b: Int = await(IOs(10_000))
    println(b)

    // Control flow
    val c: String = if await(IOs(true)) then "True branch" else "False branch"

    // Logical operations
    val d: Boolean = await(IOs(true)) && await(IOs(false))

    val e: Boolean = await(IOs(true)) || await(IOs(true))

    // Loop (for demonstration; this loop
    // won't execute its body)
    while (await(IOs(false)))
      println("looping")

    // Pattern matching
    val matchResult: String =
      await(IOs(1)) match
        case 1 => "One"
        case _ => "Other"

    println(matchResult)
  }

  KyoApp.run(programm)
  KyoApp.run(2.minutes)(programm)
}
