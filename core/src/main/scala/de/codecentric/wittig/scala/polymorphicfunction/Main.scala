package de.codecentric.wittig.scala.polymorphicfunction

object Main extends App:

  private def mn(i: Int): String  = i.toString // plain method
  private def mp[T](i: T): String = i.toString // polymorphic method

  private val fn: Int => String      = i => i.toString             // plain function
  private val fp: [T] => T => String = [T] => (i: T) => i.toString // polymorphic function:

  println(mn(1))
  println(mp(1))
  println(fn(1))
  println(fp(1))
