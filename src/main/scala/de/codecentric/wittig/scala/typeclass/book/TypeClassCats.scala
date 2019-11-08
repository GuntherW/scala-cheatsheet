package de.codecentric.wittig.scala.typeclass.book
import cats.Eq
import cats.syntax.eq._

object ViertesBeispiel extends App {
  implicit val d = Eq.instance[Dog]((a: Dog, b: Dog) => a == b)

  implicit val ec = new Eq[Cat] {
    def eqv(a: Cat, b: Cat) = a == b
  }

  val a = Cat("ewr", 234, "w34r")
  val b = Cat("ewr", 234, "w34r")

  println(a === b)
  println(Dog("2", 2, "2") === Dog("2", 2, "2"))
}
