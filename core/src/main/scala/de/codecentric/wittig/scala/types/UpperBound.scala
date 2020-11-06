package de.codecentric.wittig.scala.types

object UpperBound extends App {
  class Person(val name: String) extends Ordered[Person] {
    def compare(that: Person): Int =
      name.compareTo(that.name)
  }
  val l = List(new Person("Henning"), new Person("Wolfram"))

  orderedMergeSort(l)

  /**
    * T muss vom Trait Ordered ableiten
    */
  def orderedMergeSort[T <: Ordered[T]](xs: List[T]): List[T] = {
    def merge(xs: List[T], ys: List[T]): List[T] =
      (xs, ys) match {
        case (Nil, _)             => ys
        case (_, Nil)             => xs
        case (x :: xs1, y :: ys1) =>
          if (x < y) x :: merge(xs1, ys)
          else y :: merge(xs, ys1)
      }

    val n = xs.length / 2
    if (n == 0)
      xs
    else {
      val (ys, zs) = xs splitAt n
      merge(orderedMergeSort(ys), orderedMergeSort(zs))
    }
  }
}
