package de.codecentric.wittig.scala.directstyle

import scala.util.boundary
import scala.util.boundary.break

@main
def boundaryAndBreak(): Unit =
  val xs = List("eins", "zwei", "drei", "vier", "fünf", "sechs", "sieben", "acht", "neun", "zehn")
  assert(firstIndexBoundary(xs, "zwei") == 1)
  assert(firstIndex(xs, "zwei") == 1)
  assert(firstIndexLazy(xs, "zwei") == 1)

  def firstIndexBoundary[T](xs: List[T], elem: T): Int =
    boundary:
      for (x, i) <- xs.zipWithIndex do
        if x == elem then break(i)
      -1

  def firstIndex[T](xs: List[T], elem: T): Int =
    xs.zipWithIndex
      .find((e, _) => e == elem)
      .map(_._2)
      .getOrElse(-1)

  def firstIndexLazy[T](xs: List[T], elem: T): Int =
    xs.view.zipWithIndex
      .find((e, _) => e == elem)
      .map(_._2)
      .getOrElse(-1)
