package de.codecentric.wittig.scala.chimney

import java.util.UUID

import io.scalaland.chimney._
import io.scalaland.chimney.dsl._

final case class Person(name: String, age: Int, email: String)
final case class Customer(id: UUID, name: String, age: Int, email: String)
final case class Patch(name: String, age: Int)

object Main extends App {

  val person = Person("Henning", 28, "gunther@toll-collect.de")

  // Patching
  val patch         = Patch("Gunther", 42)
  val patchedPerson = person.patchUsing(patch)

  // Transformation
  val transformerPersonToCustomer: Transformer[Person, Customer] = person =>
    Customer(
      UUID.randomUUID(),
      person.name + "_patched",
      person.age,
      person.email
    )
  implicit val customerPatcher: Patcher[Customer, Int] = (customer, newAge) => customer.copy(age = newAge)
  val customer1 = person
    .into[Customer]
    .withFieldConst(_.id, UUID.randomUUID)
    .transform
  val customer2 = person.transformInto[Customer](transformerPersonToCustomer)
  val customer3 = customer2.patchUsing(123)
  val customer4 = customer2.patchUsing(patch)

  // printing
  println("-" * 100)
  println(person)
  println(patchedPerson)
  println("-" * 100)
  println(customer1)
  println(customer2)
  println(customer3)
  println(customer4)
  println("-" * 100)
}
