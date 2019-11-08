package de.codecentric.wittig.scala.shapeles

import shapeless._
import poly.identity

object size extends Poly1 {
  implicit def caseInt    = at[Int](x ⇒ 1)
  implicit def caseString = at[String](_.length)
  implicit def caseTuple[T, U](implicit st: Case.Aux[T, Int], su: Case.Aux[U, Int]) =
    at[(T, U)](t ⇒ size(t._1) + size(t._2))
}

object Poli extends App {
  assert(size(23) == 1)
  assert(size((23, "foo")) == 4)
}
