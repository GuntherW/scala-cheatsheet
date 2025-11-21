package de.wittig.plain

import scala.util.boundary
import scala.util.boundary.break

@main
def boundaryAndBreak(): Unit =

  val xs = List("eins", "zwei", "drei", "vier", "fünf", "sechs", "sieben", "acht", "neun", "zehn")
  assert(firstIndexBoundary(xs, "zwei") == 1)
  assert(firstIndex(xs, "zwei") == 1)
  assert(firstIndexLazy(xs, "zwei") == 1)

//  boundary defines a context for a computation.
//  break returns a value from within the enclosing boundary.
  def firstIndexBoundary[T](xs: List[T], elem: T): Int =
    boundary:
      for (x, i) <- xs.zipWithIndex do
        if x == elem then break(i) // return first index
      -1 // return -1 if elem not found

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
