package de.codecentric.wittig.scala.fp

import de.codecentric.wittig.scala.pickling.Pickler.Person

object Main extends App {

  val square: (Int, Int) => Int = (x, y) => x * x
  println(square(2, 3))
  val s = square(4, _: Int)
  println(s(5))

  def bb(a: Int, b: Int) = a
  def b = bb(2, _: Int)
  println(b(6))

  type EitherString[A] = Either[String, A]

  val a: EitherString[Int] = Left("2")

  //  trait ListMap[A] {
  //    type B
  //    val list: List[B]
  //    val mapf: B => A
  //    def run: List[A] = list.map(mapf)
  //  }

  //  class AA extends ListMap[String, Int] {
  //    override val list: List[Int] = List(1, 2, 3)
  //    override val mapf: Int => String = x => (x + 1).toString
  //  }

}