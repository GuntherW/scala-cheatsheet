package de.wittig.macros.rockthejvm.inlines

import compiletime.summonInline
import tools.*

@main
def gTupleMatching(): Unit =

  inline def showTuple[T <: Tuple](tuple: T): String =
    inline tuple match
      case EmptyTuple                => ""
      case tuple: (ht *: EmptyTuple) =>
        val h *: t = tuple
        summonInline[Show[ht]].show(h)
      case tuple: (ht *: tt)         =>
        val h *: t = tuple
        summonInline[Show[ht]].show(h) + " | " + showTuple(t)

  val aTuple = showTuple(("Scala", 2, true))
  println(aTuple)
