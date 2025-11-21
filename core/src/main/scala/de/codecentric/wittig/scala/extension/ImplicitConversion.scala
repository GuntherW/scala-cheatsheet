package de.codecentric.wittig.scala.extension

import scala.language.implicitConversions

@main
def implicitConversion(): Unit =

  given Conversion[Double, Money] = d => Money(d, "EUR")

  def sameCurrency(a: Money, b: Money) = a.currency == b.currency

  println(sameCurrency(123.0, 499.99))
