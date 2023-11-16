package de.codecentric.wittig.scala.parallelcollection
import de.codecentric.wittig.scala.Ops.time

import scala.collection.parallel.CollectionConverters.*

/** You need to include "org.scala-lang.modules" %% "scala-parallel-collections"
  */
object ParallelandSeq extends App:

  private val l1 = List.tabulate(1000)(identity)
  private val m1 = (0 to 3).map(i => (i, l1)).toMap

  private def slowIncrement(i: Int): Int =
    Thread.sleep(10)
    i + 1

  def s1 = l1
    .map(slowIncrement)

  def p1 = l1
    .par
    .map(slowIncrement)

//  time(s1.foreach(println)) // sequenziell
//  time(p1.foreach(println))     // mit zufälliger Reihenfolge
  time(p1.seq.foreach(println)) // mit gleiche Reihenfolge

  def ms1: Map[Int, List[Int]] = m1
    .map { (key, values) =>
      println(s"handling key: $key")
      key -> values.map(slowIncrement)
    }
//  ms1.foreach((key, values) => println(key))

  def mp1 = m1
    .par
    .map { (key, values) =>
      println(s"handling key: $key")
      key -> values.map(slowIncrement)
    }
  def mp2 = m1
    .par
    .map { (key, values) =>
      println(s"handling key: $key")
      key -> values.par.map(slowIncrement)
    }
//  mp1.foreach((key, values) => println(values))
//  mp2.foreach((key, values) => println(values))
