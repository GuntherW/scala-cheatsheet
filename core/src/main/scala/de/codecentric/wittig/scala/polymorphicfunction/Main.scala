package de.codecentric.wittig.scala.polymorphicfunction

@main
def main(): Unit =

  def mn(i: Int): String  = i.toString // plain method
  def mp[T](i: T): String = i.toString // polymorphic method

  val fn: Int => String      = i => i.toString             // plain function
  val fp: [T] => T => String = [T] => (i: T) => i.toString // polymorphic function:

  println(mn(1))
  println(mp(1))
  println(fn(1))
  println(fp(1))
