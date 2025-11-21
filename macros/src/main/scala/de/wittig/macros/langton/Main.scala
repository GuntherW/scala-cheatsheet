package de.wittig.macros.langton

import de.wittig.macros.langton.{into, transform}

case class Person(name: String, age: Int, alive: Boolean)
case class User(name: String, alive: Boolean, age: Int)

@main
def main(): Unit =
  val person     = Person("Gunther", 123, true)
  val user: User = person.into[User].transform
  println(s"Hallo $person")
  println(s"Hallo $user")
