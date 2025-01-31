package de.wittig.macros.rockthejvm.inlines.tools
import compiletime.*
import scala.deriving.Mirror

trait Show[A]:
  def show(a: A): String

object Show:
  given Show[Int] with
    def show(a: Int) = a.toString

  given Show[String] with
    def show(a: String) = a

  given Show[Boolean] with
    def show(a: Boolean) = a.toString

  // showTuple[(String, Int, Boolean)](("name", "age", "programmer"))(("Scala", 2, true))
  private inline def showTuple[E <: Tuple, L <: Tuple](elements: E): List[String] =
    inline (elements, erasedValue[L]) match // (("Scala", 2, true), ("name", "age", "programmer"))
      case (EmptyTuple, EmptyTuple)          => Nil
      case (el: (eh *: et), lab: (lh *: lt)) =>
        val (h *: t) = el             // h = "Scala" t =  (2, true)
        val label    = constValue[lh] // label = "name"
        val value    = summonInline[Show[eh]].show(h)
        ("" + label + ": " + value) :: showTuple[et, lt](t)

  inline def derived[A <: Product](using m: Mirror.ProductOf[A]): Show[A] =
    new Show[A] {
      override def show(entity: A): String =
        val valueTuple = Tuple.fromProductTyped(entity)
        val fieldReprs = showTuple[m.MirroredElemTypes, m.MirroredElemLabels](valueTuple)
        fieldReprs.mkString("{", ", ", "}")
    }
