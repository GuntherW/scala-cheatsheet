package de.codecentric.wittig.scala.cat

import cats.data.NonEmptyList
import cats.implicits.*
import cats.kernel.Eq
import cats.Eq

case class Person(name: String, alter: Int)

object NonEmptyListTest extends App {

  val l = NonEmptyList.fromList(List.tabulate(3)(i => Person("name" + i, i))).get

  val eqp: Eq[Person] = Eq.fromUniversalEquals

  println(l.contains_(Person("name0", 0))(eqp, implicitly))
  println(l.exists(_ == Person("name0", 0)))
  println(l)
}
