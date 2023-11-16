package de.codecentric.wittig.scala.parallelcollection

import de.codecentric.wittig.scala.Ops.time

import scala.collection.parallel.CollectionConverters.*

/** You need to include "org.scala-lang.modules" %% "scala-parallel-collections"
  */
object ParallelAndView extends App:

  private val l1 = List.tabulate(100)(identity)
  private val m1 = (0 to 3).map(i => (i, l1)).toMap

  private def slowIncrement(i: Int): Int =
    Thread.sleep(10)
    i + 1

  private def decode(key: Int, values: List[Int]): (Int, List[Int]) =
    println(s"decoding key: $key")
    key -> values.map(slowIncrement)

  private def transform(key: Int, values: List[Int]): (Int, List[Int]) =
    println(s"transforming key: $key")
    key -> values.map(slowIncrement)

  def mp1 = m1
    .par
    .map(decode)
    .map(transform)

  def mp2 = m1
    .par
    .map(decode)

  time(mp1.foreach((key, values) => println(values)))
//  time(mp2.foreach((key, values) => println(values)))
