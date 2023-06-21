package de.codecentric.wittig.scala.extension
import scala.language.implicitConversions

object Main extends App:

  case class Money(value: Double, currenty: String)

  extension (m: Money)
    def +(other: Money) = Money(m.value + other.value, m.currenty)

  given Conversion[Double, Money] = Money(_, "EUR")

  val m1 = Money(123.12, "EUR")
  val m2 = Money(123.12, "EUR")
  println(m1 + m2)

  def sameCurrency(a: Money, b: Money) = a.currenty == b.currenty
  println(sameCurrency(123.23, 123.32))
