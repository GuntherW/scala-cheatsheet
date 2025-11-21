package de.codecentric.wittig.scala.collection

import de.codecentric.wittig.scala.Ops.time
import de.codecentric.wittig.scala.Printer.printlnBlue

@main
def lazyListMain(): Unit =

  printlnBlue("LazyList")

  def isPrime(n: Int) = (2 until n).forall(n % _ != 0)
  val primes          = LazyList.from(2).filter(isPrime)

  time(println(primes.slice(10000, 10001).head)) // drop(10000).take(1)
