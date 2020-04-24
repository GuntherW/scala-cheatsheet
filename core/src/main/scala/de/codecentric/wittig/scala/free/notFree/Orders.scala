package de.codecentric.wittig.scala.free.notFree

object Orders {
  type Symbol   = String
  type Response = String
  def buy(stock: Symbol, amount: Int): Response  = ???
  def sell(stock: Symbol, amount: Int): Response = ???
}
