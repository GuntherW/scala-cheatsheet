package de.codecentric.wittig.scala.category

/**
  * @author gunther
  */
object Category {
  def id[A]: A => A                                  = a => a
  def compose[A, B, C](g: B => C, f: A => B): A => C =
    g compose f // This is Function.compose, not a recursive call!
}

object Test {
  val a = List(1, 2, 3)
  val b = List(1, 2, 3)
  a == b
}
