package de.codecentric.wittig.scala.types

/**
  * @author gunther
  * (View Bounds) https://twitter.github.io/scala_school/advanced-types.html
  */
object ViewBound extends App {
  implicit def strToInt(s: String) = s.toInt

  viewBounds()
  view()

  // View bounds, like type bounds demand such a function exists for the given type.
  // You specify a view bound with <% e.g.,
  def viewBounds(): Unit = {
    class Container[A](implicit toInt: A => Int) { // class Container[A <% Int]
      def addInt(x: A): Int = 123 + x
    }
    println(new Container[String].addInt("1"))
    println(new Container[Int].addInt(1))
  }

  // Mit "normalem" Upper Bound Type <: gibt es einen Compilerfehler
  def view(): Unit = {
    class Container[A <: Int] {
      def addInt(x: A): Int = 123 + x
    }
    //    println(new Container[String].addInt("1")) // Compilefehler
    println(new Container[Int].addInt(1))
  }
}
