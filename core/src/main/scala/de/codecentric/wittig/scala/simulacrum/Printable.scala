package de.codecentric.wittig.scala.simulacrum
import simulacrum._

/**
  * Type class definition
  *
  * @tparam A
  */
@typeclass
trait Printable[A] {
  def asString(a: A): String
}

object Printable {
  implicit def personPrinterN: Printable[Person] = a => s"Name: ${a.name}, alter: ${a.alter}"
  implicit def stringPrinter: Printable[String]  = a => a.toLowerCase
}
