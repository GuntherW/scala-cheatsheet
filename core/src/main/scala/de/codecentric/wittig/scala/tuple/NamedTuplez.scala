package de.codecentric.wittig.scala.tuple

import scala.deriving.Mirror
import scala.util.chaining.*
import NamedTuple.withNames

type Person = (name: String, age: Int)

@main
def namedTuples(): Unit =

  val bob: Person = (name = "Bob", age = 33)
  println(bob.name)

  bob match
    case (name = "Bob", age = 33)   => println(s"Hallo ${bob.name}, matched 33")
    case (age = 33, name = "Bobby") => println(s"Hallo ${bob.name}, matched 33") // swap order is allowed
    case (name = "Bob")             => println(s"Hallo Bob, matched 33")         // forget fields is allowed
    case (name, 33)                 => println(s"Hallo $name, matched 33")
    case (name, age)                => println(s"Hallo $name, $age")

  // map and filter
  List(bob, ("Alice", 22), ("Steve", 25))
    .filter(_.age > 22)
    .mkString(", ")
    .tap(println)

  // conversion
  val p: Person = ("Peter", 22).withNames[("name", "age")]
  assert(p(1) == p.age)
  summon[Mirror.Of[Person]].fromProduct(p.toTuple)

  val nameT = (name = "Bob")
  val ageT  = (age = 22)
  val bob2  = nameT ++ ageT
//  val wontCompile = ageT ++ ageT

  case class PersonV1(name: String, age: Int)
  case class PersonV2(name: String, age: Int, imker: Boolean)

  val p1 = PersonV1("gun", 44)

  p1 match
    case PersonV1(name = "gun") => println("match") // Pattern matching with named fields
    case _                      => println("no match")

//  val p2 = p1
//    .asNamedTuple
//    .withField((imker = true))
//    .as[PersonV2]
//
//  println(s"Converted case class: $p2")
