package de.codecentric.wittig.scala.opaque

object Main extends App:

  opaque type Year <: Int = Int

  object Year:
    def apply(i: Int): Year        = i
    def make(i: Int): Option[Year] = if i < 10 then Some(i) else None

  extension (y: Year) {
    def pprint = s"--$y--"
  }

  case class Person(name: String, year: Year)
  case class Person2(name: String, year: Int)

  val p  = Person("lkj", Year(123))
  val p2 = Person2("lkj", Year(123))
  println(p)
  println(p2)
  println(Year.make(5))
  println(Year.make(5).map(_.pprint))
