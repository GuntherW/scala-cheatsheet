package de.codecentric.wittig.scala.types.varianz

/** @author
  *   gunther
  */
@main
def varianz(): Unit =
  class Covariant[+A]
  val cov: Covariant[AnyRef] = new Covariant[String]
  // val fail: Covariant[String] = new Covariant[AnyRef]

  class Contravariant[-A]
  val con: Contravariant[String] = new Contravariant[AnyRef]
  // val fail: Contravariant[AnyRef] = new Contravariant[String]
