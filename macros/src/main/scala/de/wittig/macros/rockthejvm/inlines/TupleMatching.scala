package de.wittig.macros.rockthejvm.inlines

import compiletime.summonInline
import tools.*

object TupleMatching extends App:

  inline def showTuple[T <: Tuple](tuple: T): String =
    inline tuple match
      case EmptyTuple              => ""
      case tup: (ht *: EmptyTuple) =>
        val h *: t = tup
        summonInline[Show[ht]].show(h)
      case tup: (ht *: tt)         =>
        val h *: t = tup
        summonInline[Show[ht]].show(h) + ", " + showTuple(t)

  val aTuple = showTuple(("Scala", 2, true))
  println(aTuple)
