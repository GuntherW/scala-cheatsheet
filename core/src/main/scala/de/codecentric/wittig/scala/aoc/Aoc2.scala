package de.codecentric.wittig.scala.aoc

import scala.io.{BufferedSource, Source}

import de.codecentric.wittig.scala.aoc.Input.*
import scala.util.chaining.*
import cats.implicits.*
import monocle.syntax.all.*

object Aoc2 extends App {

  private val testInput = "aoc/2/testData.txt".getLines
  private val input     = "aoc/2/data.txt".getLines
  private val Regex     = """(\w+) (\d+)""".r

  println(part1(input))
  println(part2(input).result)

  private def part1(list: List[String]) = {
    val m: Map[String, BigInt] = list
      .collect { case Regex(direction, amount) => Map(direction -> BigInt(amount)) }
      .combineAll
      .withDefaultValue(BigInt(0))

    val forward = m("forward")
    val up      = m("up")
    val down    = m("down")
    (forward, down - up).pipe((a, b) => (a, b, a * b))
  }

  private def part2(list: List[String]) =
    list
      .collect { case Regex(direction, amount) => direction -> BigInt(amount) }
      .foldLeft(Sub(0, 0, 0)) {
        case (sub, ("forward", amount)) => sub.focus(_.horizontal).modify(_ + amount).focus(_.depth).modify(_ + sub.aim * amount)
        case (sub, ("up", amount))      => sub.focus(_.aim).modify(_ - amount)
        case (sub, ("down", amount))    => sub.focus(_.aim).modify(_ + amount)
      }
}

case class Sub(aim: BigInt, horizontal: BigInt, depth: BigInt) {
  def result: BigInt = horizontal * depth
}
