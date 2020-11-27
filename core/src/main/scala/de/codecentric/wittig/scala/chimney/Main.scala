package de.codecentric.wittig.scala.chimney

import java.util.UUID

import io.scalaland.chimney._
import io.scalaland.chimney.dsl._

final case class Person(name: String, age: Int, email: String)
final case class PersonPatch(name: String, age: Int)
final case class Customer(id: UUID, name: String, alter: Int, email: String)

object Main extends App {

  val person = Person("Henning", 28, "gunther@toll-collect.de")

  // Patching an Object
  val patch         = PersonPatch("Gunther", 42)
  val patchedPerson = person.patchUsing(patch)
  println("-" * 100)
  println(person)
  println(patchedPerson)
  println("-" * 100)

  // Transform a Person into a Customer - Way 1
  val customer1 = person
    .into[Customer]
    .withFieldConst(_.id, UUID.randomUUID)
    .withFieldRenamed(_.age, _.alter)
    .transform
  println(customer1)

  // Transform a Person into a Customer - Way 2
  val transformerPersonToCustomer: Transformer[Person, Customer] = person =>
    Customer(
      UUID.randomUUID(),
      person.name + "_patched",
      person.age,
      person.email
    )
  val customer2                                                  = person.transformInto[Customer](transformerPersonToCustomer)
  println(customer2)

  implicit val customerPatcher: Patcher[Customer, Int] =
    (customer, newAge) => customer.copy(alter = newAge)

  val customer3 = customer2.patchUsing(123)
  println(customer3)
  println("-" * 100)
}
