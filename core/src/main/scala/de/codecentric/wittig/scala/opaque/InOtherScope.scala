package de.codecentric.wittig.scala.opaque

object UtilScope:

  opaque type Month <: Int = Int
  object Month:
    def apply(i: Int): Month = i

  opaque type Year <: Int = Int
  object Year:
    def apply(i: Int): Year        = i
    def make(i: Int): Option[Year] = if i < 10 then Some(i) else None

  extension (y: Year)
    def pprint = s"--$y--"

@main
def otherScope(): Unit =
  import UtilScope.*

  case class Person(name: String, year: Year)
  case class Person2(name: String, year: Int)

  println(Person("lkj", Year(123)))
  //  println(Person("lkj", Month(123))) // Kompiliert nicht, weil der opaque Typ in einem anderen Scope definiert wurde
  println(Person2("lkj", Month(123)))
