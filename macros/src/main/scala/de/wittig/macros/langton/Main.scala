package de.wittig.macros.langton

import de.wittig.macros.langton.{into, transform}
case class Person(name: String, age: Int, alive: Boolean)
case class User(name: String, alive: Boolean, age: Int)

def person2User(person: Person): User =
  person.into[User].transform

object Main extends App:
  val person     = Person("Gunther", 123, true)
  val user: User = person2User(person)
  println(s"Hallo $user")
