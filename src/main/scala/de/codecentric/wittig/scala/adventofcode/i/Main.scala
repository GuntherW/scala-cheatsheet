package de.codecentric.wittig.scala.adventofcode.i

import scala.io.Source

object Main extends App {
  val lines                = Source.fromResource("input.txt").getLines().toList
  val fuel                 = lines.map(_.toInt).map(calc).sum
  def calc(mass: Int): Int = (mass / 3) - 2

  println(s"fuel: $fuel")
}
