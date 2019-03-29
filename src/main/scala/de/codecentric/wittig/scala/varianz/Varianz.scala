package de.codecentric.wittig.scala.varianz

/**
  * @author gunther
  */
object Varianz extends App {

  class Covariant[+A]
  val cov: Covariant[AnyRef] = new Covariant[String]
  //val cv: Covariant[String] = new Covariant[AnyRef]

  class Contravariant[-A]
  val con: Contravariant[String] = new Contravariant[AnyRef]
  //val fail: Contravariant[AnyRef] = new Contravariant[String]

}
