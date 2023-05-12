package de.codecentric.wittig.scala.directstyle

import scala.util.boundary
import scala.util.boundary.break

object BoundaryAndBreak extends App:

  private val xs = List("eins", "zwei", "drei", "vier", "fünf", "sechs", "sieben", "acht", "neun", "zehn")
  assert(firstIndexBoundary(xs, "zwei") == 1)
  assert(firstIndex(xs, "zwei") == 1)
  assert(firstIndexLazy(xs, "zwei") == 1)

  private def firstIndexBoundary[T](xs: List[T], elem: T): Int =
    boundary:
      for (x, i) <- xs.zipWithIndex do
        if x == elem then break(i)
      -1

  private def firstIndex[T](xs: List[T], elem: T): Int =
    xs.zipWithIndex
      .find((e, _) => e == elem)
      .map(_._2)
      .getOrElse(-1)

  private def firstIndexLazy[T](xs: List[T], elem: T): Int =
    xs.view.zipWithIndex
      .find((e, _) => e == elem)
      .map(_._2)
      .getOrElse(-1)
