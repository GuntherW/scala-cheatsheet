package de.codecentric.wittig.scala.shapeles.generics

import shapeless.LabelledGeneric
import shapeless.record._
import shapeless.syntax.singleton.mkSingletonOps

case class Model(name: String, alter: Int)

object Main extends App {
  val gen = LabelledGeneric[Model]

  val m = Model("Hans", 22)

  val repr1 = gen.to(m)
  val repr2 = repr1 + ('name ->> "Heinz")

  println("m " + m)
  println("repr1 " + repr1)
  println("repr2 " + repr2)
}
