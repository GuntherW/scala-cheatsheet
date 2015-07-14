package de.codecentric.wittig.scala

import shapeless.ops.record.MapValues

import scala.collection.breakOut
/**
 * @author gunther
 */
object Quiz extends App {
  import scala.collection.breakOut

  val map: Map[Int, Char] = List(1 -> 'a', 2 -> 'B')

    .map { case (k, v) => k -> v.toLower }(breakOut)

  println(map)

}
