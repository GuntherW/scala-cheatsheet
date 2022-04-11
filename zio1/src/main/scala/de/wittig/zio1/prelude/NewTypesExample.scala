package de.wittig.zio1.prelude

import scala.io.StdIn.readLine

import zio.prelude.*
import Assertion.*
import Person.*

object NewTypesExample extends App {

  val name1    = Name("MeinName")
  val address1 = Address("MeineAdresse")
  val email1   = Email("a@b.de")
  val person1  = Person(name1, email1, address1)
  println(person1)
  println(name1.toUpperCase) // Because Name is "Subtype" of String, we can use it as a String anywhere

  val name2    = Name.make(readLine())
  val email2   = Email.make(readLine())
  val address2 = Address.make(readLine())

  // short circuiting
  val person2a: Validation[String, Person] =
    for
      name    <- name2
      email   <- email2
      address <- address2
    yield Person(name, email, address)

  // validating in parallel
  val person2b = Validation.validateWith(name2, email2, address2)(Person.apply)
  println(person2a)
}

case class Person(name: Name, email: Email, address: Address)

object Person {
  object Name extends Subtype[String]:
    override inline def assertion: Assertion[String] = !isEmptyString
  type Name = Name.Type

  object Address extends Subtype[String]:
    override inline def assertion: Assertion[String] = !isEmptyString
  type Address = Address.Type

  object Email extends Subtype[String]:
    override inline def assertion: Assertion[String] = matches(".+@.+".r)
  type Email = Email.Type
}
