package de.codecentric.wittig.scala.free
import cats.free.Free
import cats.free.Free._
import de.codecentric.wittig.scala.free.Orders._

object Orders {
  type Symbol   = String
  type Response = String
  def buy(stock: Symbol, amount: Int): OrdersF[Response]  = liftF[Orders, Response](Buy(stock, amount))
  def sell(stock: Symbol, amount: Int): OrdersF[Response] = liftF[Orders, Response](Sell(stock, amount))
  type OrdersF[A] = Free[Orders, A]
}

sealed trait Orders[A]
case class Buy(stock: Symbol, amount: Int) extends Orders[Response]
case class Sell(stock: Symbol, amount: Int) extends Orders[Response]
