package de.codecentric.wittig.scala.collection

import de.codecentric.wittig.scala.Ops.time
import de.codecentric.wittig.scala.Printer.printlnBlue

object LazyListMain extends App:

  printlnBlue("LazyList")

  val primes          = LazyList.from(2).filter(isPrime)
  def isPrime(n: Int) = (2 until n).forall(n % _ != 0)

  val primes2: LazyList[Int] = 2 #:: LazyList.from(3, 2).filter(isPrime2)
  def isPrime2(x: Int)       = primes2.takeWhile(p => p * p <= x).forall(x % _ != 0)

  time(println(primes.slice(10000, 10001).head)) // drop(10000).take(1)
  time(println(primes2.slice(10000, 10001).head))
