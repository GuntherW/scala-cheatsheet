package de.codecentric.wittig.scala.ordered

object WithOrdering extends App:
  case class CurrencyAmount(amount: Double, currency: String)

  given o1: Ordering[CurrencyAmount] = Ordering.by(_.amount)
  val o2: Ordering[CurrencyAmount]   = Ordering.by[CurrencyAmount, Double](_.amount).reverse
  val o3: Ordering[CurrencyAmount]   = Ordering.fromLessThan(_.amount < _.amount)
  val o4: Ordering[CurrencyAmount]   = Ordering.fromLessThan(_.amount > _.amount)

  val a = CurrencyAmount(123, "EUR")
  val b = CurrencyAmount(124, "EUR")

  println(List(b, a).sorted)
  println(List(b, a).sorted(o2))
  println(List(b, a).sorted(o3))
  println(List(b, a).sorted(o4))
