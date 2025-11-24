package de.codecentric.wittig.scala.extractors

import scala.util.Random

@main
def main(): Unit =

  object Even:
    def unapply(s: String): Boolean = s.length % 2 == 0

  "even" match
    case s @ Even() => println(s"$s has even number of characters")
    case s          => println(s"$s has odd number of characters")

  object CustomerID:
    def apply(name: String) = s"$name--${Random.nextLong()}"

    def unapply(customerID: String): Option[String] =
      val stringArray: Array[String] = customerID.split("--")
      if stringArray.tail.nonEmpty then Some(stringArray.head) else None

  val customer1ID = CustomerID("Ka")
  customer1ID match
    case CustomerID(name) => println(name)
    case _                => println("Could not extract a CustomerID")

  case class User(name: String, age: Int)

  val user = User.apply("Hallo", 123)
  val b    = User.unapply(user)
  user match
    case User(name, age) => println(name)
