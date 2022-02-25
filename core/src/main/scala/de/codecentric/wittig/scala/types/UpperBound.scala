package de.codecentric.wittig.scala.types

import de.codecentric.wittig.scala.Printer.printlnYellow

object UpperBound extends App:
  case class Person(name: String) extends Ordered[Person]:
    def compare(that: Person): Int = name.compareTo(that.name)

  val l = List(Person("Henning"), Person("Wolfram"), Person("Alwin"))

  printlnYellow(orderedMergeSort(l).map(_.name))

  /** T muss vom Trait Ordered ableiten
    */
  def orderedMergeSort[T <: Ordered[T]](xs: List[T]): List[T] =
    def merge(xs: List[T], ys: List[T]): List[T] =
      (xs, ys) match
        case (Nil, _)             => ys
        case (_, Nil)             => xs
        case (x :: xs1, y :: ys1) =>
          if x < y then x :: merge(xs1, ys)
          else y :: merge(xs, ys1)

    val n = xs.length / 2
    if n == 0 then
      xs
    else
      val (ys, zs) = xs splitAt n
      merge(orderedMergeSort(ys), orderedMergeSort(zs))
