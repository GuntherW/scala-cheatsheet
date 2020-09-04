package de.codecentric.wittig.scala.memoize

object Memo extends App {
  def memoize[A, B](f: A => B) =
    new (A => B) {
      val cache = scala.collection.mutable.Map[A, B]()
      def apply(x: A) = {
        cache.getOrElseUpdate(x, f(x))
      }
    }

  def normalize(c: Char): Char = {
    val x  = c.toInt - 65
    val t  = x % 26
    val t2 = t + 65
    t2.toChar
  }

  def rot(offset: Int)(c: Char) = {
    val ro = memoize[Char, Char] { x =>
      (x + offset).toChar
    }
    normalize(ro(c))
  }
  val rot13                    = rot(13)(_)

  println(rot13('a'))
}
