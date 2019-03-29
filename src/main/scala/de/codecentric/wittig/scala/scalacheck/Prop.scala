package de.codecentric.wittig.scala.scalacheck

class Prop {
  def check(): Unit = ???
}
object Prop {
  def forAll[A](prop: A => Boolean) = ???
}

object Main {
  Prop.forAll { (x: Int) =>
    x > 2
  }
}
