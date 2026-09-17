package de.wittig.macros.timed

import scala.annotation.experimental

/** Demo for the @timed macro annotation.
  *
  * Run with: sbt --client "macros/runMain de.wittig.macros.timed.runTimedDemo"
  */

@timed @experimental
def sum(n: Int): Long = (1L to n).sum

@timed @experimental
def slowSort(list: List[Int]): List[Int] =
  Thread.sleep(50)
  list.sorted

@timed @experimental
def fibonacci(n: Int): Long =
  if n <= 1 then n.toLong
  else fibonacci(n - 1) + fibonacci(n - 2)

@main @experimental def runTimedDemo(): Unit =
  println("=== @timed Macro Demo ===\n")

  val s = sum(10_000_000)
  println(s"  result: $s\n")

  val sorted = slowSort(List(5, 3, 8, 1, 9, 2, 7, 4, 6))
  println(s"  result: $sorted\n")

  val fib = fibonacci(35)
  println(s"  result: $fib\n")
