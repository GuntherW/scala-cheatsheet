package de.wittig.datatransformation.chimney

import io.scalaland.chimney.dsl._

import Dtos.*
import model.*

object Main extends App:

  private val user = User(
    Username("John"),
    List(Address("Paper St", "Somewhere")),
    RecoveryMethod.Email("john@example.com")
  )

  println(user.transformInto[UserDTO])

object Dtos:
  case class UserDTO(
      name: String,               // 1. primitive
      addresses: Seq[AddressDTO], // 2. Seq collection
      recovery: Option[RecoveryMethodDTO] // 3. Option type
  )
  case class AddressDTO(street: String, city: String)

  // 4. ADT is not flat - each oneOf message created 2 case classes
  sealed trait RecoveryMethodDTO
  object RecoveryMethodDTO {
    case class Phone(value: PhoneDTO) extends RecoveryMethodDTO
    case class Email(value: EmailDTO) extends RecoveryMethodDTO
  }

  case class PhoneDTO(number: String)
  case class EmailDTO(email: String)

object model:
  case class User(
      name: Username,           // 1. value class
      addresses: List[Address], // 2. List collection
      recovery: RecoveryMethod // 3. non-Option type
  )
  case class Username(name: String) extends AnyVal
  case class Address(street: String, city: String)

  // 4. flat enum
  enum RecoveryMethod:
    case Phone(number: String)
    case Email(email: String)
