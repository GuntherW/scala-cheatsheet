package de.codecentric.wittig.scala.cat.fmap

import cats.implicits.*

@main
def main(): Unit =

  case class Person(name: String, age: Int)
  val l = List(Person("lkj", 22), Person("jj", 44), Person("a", 22))

  println(l)
  println(l.map(_.age * 2))

  println(l.groupBy(_.age).view.mapValues(_.map(_.age * 2)).toMap)
  println(l.groupBy(_.age).fmap(_.map(_.age * 2)))
  println(l.groupBy(_.age).fmap(_.map(_.age).combineAll))
  println(l.groupBy(_.age).fmap(_.foldMap(_.age)))
  println(l.groupBy(_.age).fmap(_.foldMap(_.age)))
  println(l.groupBy(_.age))
  println(l.groupByNel(_.age).map(i => Option(i)))
  println(l.groupByNel(_.age).traverse(i => Option(i)))
