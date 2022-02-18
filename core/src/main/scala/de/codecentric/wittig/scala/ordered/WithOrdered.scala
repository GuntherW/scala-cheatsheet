package de.codecentric.wittig.scala.ordered

object WithOrdered extends App:
  case class CurrencyAmount(amount: Double, currency: String) extends Ordered[CurrencyAmount]:
    override def compare(that: CurrencyAmount): Int = this.amount.compareTo(that.amount)

  val a = CurrencyAmount(123, "EUR")
  val b = CurrencyAmount(124, "EUR")
  println(s"a<b: ${a < b}")
