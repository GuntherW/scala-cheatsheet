package de.codecentric.wittig.scala.monocle.devinsideyou

import monocle.macros.GenLens
import monocle.macros.syntax.lens._

case class Person(address: Address)
case class Address(street: Street, city: City)
case class Street(name: String)
case class City(zipCode: Int, name: String)

object MyLens {

  private val p  = Person(Address(Street("Holzweg"), City(12345, "Glücksstadt")))
  private val p1 = GenLens[Person](_.address.city.zipCode).set(11111)(p)
  private val p2 = p.lens(_.address.city.zipCode).set(11111)
  private val p3 = p.lens(_.address.city.zipCode).modify(_ + 1)

  def run(): Unit = {
    println("-" * 40 + "Lenses" + "-" * 40)
    println(p1)
    println(p2)
    println(p3)
  }
}
