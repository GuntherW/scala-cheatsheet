package de.codecentric.wittig.scala.algebraicdatatype

/** @author
  *   gunther
  *
  * Ein algebraic data type ist eine Kombi aus einer is A or B und einer has an A and B Beziehung.
  *
  * Hier: Ein Visitor ist ein Anonymous oder ein User. Ein User hat eine id und einen name
  */
sealed trait Visitor

final case class Anonymous(id: Int) extends Visitor

final case class User(id: Int, name: String) extends Visitor
