package de.codecentric.wittig.scala.extension

case class Money(value: Double, currency: String)
@main
def main(): Unit =

  extension (m: Money)
    def +(other: Money) = Money(m.value + other.value, m.currency)

  given Conversion[Double, Money] = value => Money(value, "EUR")

  val m1 = Money(123.12, "EUR")
  val m2 = Money(100, "EUR")
  println(m1 + m2)
