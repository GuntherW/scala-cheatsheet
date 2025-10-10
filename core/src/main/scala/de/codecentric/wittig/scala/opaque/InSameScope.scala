package de.codecentric.wittig.scala.opaque

object InSameScope extends App:

  opaque type Month <: Int = Int
  object Month:
    def apply(i: Int): Month = i

  opaque type Year <: Int = Int
  object Year:
    def apply(i: Int): Year        = i
    def make(i: Int): Option[Year] = if i < 10 then Some(i) else None

  extension (y: Year)
    def pprint = s"--$y--"

  case class Person(name: String, year: Year)
  case class Person2(name: String, year: Int)

  val p  = Person("lkj", Year(123))
  val p1 = Person("lkj", Month(123)) // Kompiliert, weil der opaque Typ im selben Scope definiert wurde
  val p2 = Person2("lkj", Month(123))

  println(p)
  println(p1)
  println(p2)
  println(Year.make(5))
  println(Year.make(5).map(_.pprint))
