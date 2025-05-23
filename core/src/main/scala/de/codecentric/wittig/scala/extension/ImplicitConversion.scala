package de.codecentric.wittig.scala.extension

import de.codecentric.wittig.scala.extension.Main.Money
import scala.language.implicitConversions

object ImplicitConversion extends App:

  given Conversion[Double, Money] = d => Money(d, "EUR")

  private def sameCurrency(a: Money, b: Money) = a.currency == b.currency

  println(sameCurrency(123.0, 499.99))
