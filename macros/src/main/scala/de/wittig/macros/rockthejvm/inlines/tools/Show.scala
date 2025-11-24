package de.wittig.macros.rockthejvm.inlines.tools
import scala.compiletime.*
import scala.deriving.Mirror

trait Show[A]:
  def show(a: A): String

object Show:

  given Show[Int]:
    def show(a: Int): String = a.toString

  given Show[String]:
    def show(a: String): String = a

  given Show[Boolean]:
    def show(a: Boolean): String = a.toString

  extension [A](a: A)(using s: Show[A])
    def show: String = s.show(a)

  // showTuple[(String, Int, Boolean)](("name", "age", "programmer"))(("Scala", 2, true))
  private inline def showTuple[E <: Tuple, L <: Tuple](elements: E): List[String] =
    inline (elements, erasedValue[L]) match // (("Scala", 2, true), ("name", "age", "programmer"))
      case (EmptyTuple, EmptyTuple)          => Nil
      case (el: (eh *: et), lab: (lh *: lt)) =>
        val h *: t = el                             // h = "Scala" t =  (2, true)
        val label  = constValue[lh]                 // label = "name"
        val value  = summonInline[Show[eh]].show(h) // value = "Scala"
        ("" + label + ": " + value) :: showTuple[et, lt](t)

  inline def derived[A <: Product](using m: Mirror.ProductOf[A]): Show[A] =
    (a: A) =>
      val valueTuple = Tuple.fromProductTyped(a)
      val fieldReprs = showTuple[m.MirroredElemTypes, m.MirroredElemLabels](valueTuple)
      fieldReprs.mkString("{", ", ", "}")
