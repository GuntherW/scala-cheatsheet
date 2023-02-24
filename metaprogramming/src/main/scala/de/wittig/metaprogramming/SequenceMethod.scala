package de.wittig.metaprogramming

import scala.Tuple.InverseMap

/** https://www.youtube.com/watch?v=leIB5tvDY64&t=1030s
  */
object SequenceMethod extends App:

  (10, "x", true): (Int, String, Boolean)
  (10, "x", true): Int *: (String, Boolean)

  type TupleMap[T <: Tuple, F[_]] <: Tuple = T match {
    case EmptyTuple => EmptyTuple
    case h *: t     => F[h] *: TupleMap[t, F]
  }

  (Some(10), None, Some(true)): (Option[Int], Option[String], Option[Boolean])
  (Some(10), None, Some(true)): TupleMap[(Int, String, Boolean), Option]

  def sequene[T <: Tuple](t: T): Option[InverseMap[T, Option]] = {
    val unwrapped = t.productIterator.collect { case Some(x) => x }.toArray[Any]
    if (unwrapped.length == t.productArity)
      Some(Tuple.fromArray(unwrapped).asInstanceOf[InverseMap[T, Option]])
    else
      None
  }

  println("Hallo Welt")
