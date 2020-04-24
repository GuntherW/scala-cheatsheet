package de.codecentric.wittig.scala.applyAndUpdate

case class Person(name: String, age: Int)

class PersonApply(name: String) {
  def apply(age: Int): Person = Person(name, age)
}

object MainApply extends App {
  val personApply = new PersonApply("hannes")
  val person      = personApply(22)
  println(person)
}
