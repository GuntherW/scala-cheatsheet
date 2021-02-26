package de.wittig.scala.scalacheck.helper

case class Person(
    firstName: String,
    lastName: String,
    age: Int,
    season: Season
) {
  def birthday: Person = copy(age = age + 1)
}

sealed trait Season
object Season {
  case object Winter extends Season
  case object Spring extends Season
  case object Summer extends Season
  case object Autumn extends Season
}
