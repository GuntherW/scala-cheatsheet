package de.codecentric.wittig.scala.monoid

import scalaz.Monoid
import scalaz.syntax.monoid._
import scalaz.std._

object MonoidSkalaz extends App {

  implicit val bitwiseXor = Monoid.instance[Int](_ ^ _, 0)

  println(5 |+| 4) // 1
  println(6 |+| 4) // 2
  println(6 |+| 3) // 5
}