package de.codecentric.wittig.scala.cats

import cats._
import cats.instances.list._
import cats.instances.map._
import cats.instances.string._
import cats.instances.tuple._
import cats.syntax.monoid._

object WithOwnIntMonoid extends App {

  implicit val m = new Monoid[Int] {
    override def empty: Int = 1
    override def combine(x: Int, y: Int): Int = x * y
  }

  println(3 |+| 3)

  val map1 = Map(1 -> 1, 2 -> 2, 3 -> 3)
  val map2 = Map(2 -> 2, 3 -> 3)

  println(map1 ++ map2)
  println(map1 |+| map2)

  val tup1 = (2, "hallo ", map1)
  val tup2 = (4, "welt", map2)
  println(tup1 |+| tup2)

  val list1 = List(1, 2, 3)
  val list2 = List(3, 4, 5)
  println(list1 |+| list2)

}
