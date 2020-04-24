package de.codecentric.wittig.scala.catsRelated

import cats.implicits._

object Standard extends App {
  combineTwoInts()
  combineTwoMaps()
  combineTuples()
  combineLists()

  def combineTwoInts(): Unit = {
    println(3 |+| 3)
  }

  def combineTwoMaps(): Unit = {
    val map1 = Map(1 -> 1, 2 -> 2, 3 -> 3)
    val map2 = Map(2 -> 2, 3 -> 3)

    println(map1 ++ map2)
    println(map1 |+| map2)
  }

  def combineTuples(): Unit = {
    val map1 = Map(1 -> 1, 2 -> 2, 3 -> 3)
    val map2 = Map(2 -> 2, 3 -> 3)

    val tup1 = (2, "hallo ", map1)
    val tup2 = (4, "welt", map2)
    println(tup1 |+| tup2)
  }

  def combineLists(): Unit = {
    val list1 = List(1, 2, 3)
    val list2 = List(3, 4, 5)
    println(list1 |+| list2)

    println(List(Option(1), Option(2), Option(3)).traverse(identity))
    println(List(Option(1), Option(2), Option(3)).sequence)

    println(List(Option(1), None, Option(3)).sequence)
  }
}
