package de.codecentric.wittig.scala.aoc

import scala.io.{BufferedSource, Source}

import de.codecentric.wittig.scala.aoc.Input.*
import scala.util.chaining.*

import cats.implicits.*
import cats.syntax.foldable.toFoldableOps
import monocle.syntax.all.*
import cats.implicits.*
import monocle.syntax.all.*

object Aoc3 extends App {

  private lazy val testInput = "aoc/3/testData.txt".getLines
  private lazy val input     = "aoc/3/data.txt".getLines

  println(part1(testInput).calculate)
  println(part1(input).calculate)

  private def part1(list: List[String]) = {
    val l = list
      .map(_.zipWithIndex.map(_.swap))
      .foldLeft(List.tabulate(list.head.length)(i => Count(i, 0, 0))) {
        case (counts, tuples) =>
          counts.zipWithIndex.map { case (count, index) =>
            if (tuples(index)._2 == '0')
              count.focus(_.zero).modify(_ + 1)
            else
              count.focus(_.one).modify(_ + 1)
          }
      }

    val gamma   = l.map(_.gamma.toString).mkString
    val epsilon = l.map(_.epsilon.toString).mkString
    PowerConsumption(gamma, epsilon)
  }
}

case class Count(index: Int, zero: BigInt, one: BigInt) {
  val gamma: Int   = if (zero > one) 0 else 1
  val epsilon: Int = if (zero < one) 0 else 1
}

case class PowerConsumption(gamma: String, epsilon: String) {
  private val gammaInt   = BigInt.int2bigInt(Integer.parseInt(gamma, 2))
  private val epsilonInt = BigInt.int2bigInt(Integer.parseInt(epsilon, 2))
  def calculate: BigInt  = gammaInt * epsilonInt
}
