package de.codecentric.wittig.scala.monoid

/**
 *
 * @author gunther
 */
object MonoidApp extends App {

  /**
   * Hat einen "Null"-Methode
   * Hat eine Verbindungsmethode, die assoziativ verbindet (a+(b+c)) == ((a+b)+c)
   */
  trait Monoid[A] {
    def zero: A
    def op(a1: A, a2: A): A
  }

  //z.B. für String
  val stringMonoid = new Monoid[String] {
    def zero = ""
    def op(s1: String, s2: String) = s1 + s2
  }

  val l = List("Hallo", "schöne", "Welt")

  println(l.foldLeft(stringMonoid.zero)(stringMonoid.op))
}