package de.codecentric.wittig.scala.cats.nested

import cats.data.Nested
import cats.implicits._

object NestedMain extends App {

  val a = 1.some.asRight[String]

  val res1 = a.map(_.map(_ + 1))
  val res2 = Nested(a).map(_ + 1).value
  val res3 = a.nested.map(_ + 1).value

  println(res1)
  println(res2)
  assert(res2 == res3)
}
