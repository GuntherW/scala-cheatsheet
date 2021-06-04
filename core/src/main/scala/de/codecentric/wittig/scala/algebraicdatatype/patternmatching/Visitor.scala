package de.codecentric.wittig.scala.algebraicdatatype.patternmatching

/** @author
  *   gunther
  *
  * Einem algebraic data type kann man über Patternmatching oder über Polymorphismus benutzen
  */
sealed trait Visitor {
  def sagHallo: String = {
    this match {
      case Anonymous(_) => "Hallo"
      case User(_, n)   => s"Hallo $n"
    }
  }
}

final case class Anonymous(id: Int) extends Visitor

final case class User(id: Int, name: String) extends Visitor
