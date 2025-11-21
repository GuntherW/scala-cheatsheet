package de.codecentric.wittig.scala.ordered

@main
def withOrdering(): Unit =
  case class CurrencyAmount(amount: Double, currency: String)

  given o1: Ordering[CurrencyAmount] = Ordering.by(_.amount)
  val o2: Ordering[CurrencyAmount]   = Ordering.by[CurrencyAmount, Double](_.amount).reverse
  val o3: Ordering[CurrencyAmount]   = Ordering.fromLessThan(_.amount < _.amount)
  val o4: Ordering[CurrencyAmount]   = Ordering.fromLessThan(_.amount > _.amount)

  val a = CurrencyAmount(123, "EUR")
  val b = CurrencyAmount(124, "EUR")

  println(List(b, a).sorted)
  println(List(b, a).sorted(using o2))
  println(List(b, a).sorted(using o3))
  println(List(b, a).sorted(using o4))
