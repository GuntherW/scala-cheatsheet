package de.wittig.macros.langton

import de.wittig.macros.langton.{into, transform}

case class Person(name: String, age: Int, alive: Boolean)
case class User(name: String, alive: Boolean, age: Int)

object Main extends App:
  private val person     = Person("Gunther", 123, true)
  private val user: User = person.into[User].transform
  println(s"Hallo $person")
  println(s"Hallo $user")
