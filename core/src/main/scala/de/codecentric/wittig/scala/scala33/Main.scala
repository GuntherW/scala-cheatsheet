package de.codecentric.wittig.scala.scala33

import scala.concurrent.duration.DurationInt

object Main extends App:

  private val l  = List.tabulate(5)(identity)
  private val l2 = l.map: x =>
    x * x
  println(l2)

  def times(n: Int)(f: => Unit): Unit =
    for i <- 0 until n do f

  times(3):
    println("Hallo Welt")
