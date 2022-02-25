package de.codecentric.wittig.scala.model

/** @author
  *   gunther
  */
trait Auth:
  self: Person =>

  def authenticate(name: String): Boolean =
    self.name == name
