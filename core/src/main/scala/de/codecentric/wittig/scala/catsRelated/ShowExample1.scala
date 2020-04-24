package de.codecentric.wittig.scala.catsRelated
import cats.Show
import cats.derived.semi
import cats.implicits._

object ShowExample1 extends App {
  case class Address(street: String, city: String, state: String)
  case class ContactInfo(phoneNumber: String, address: Address)
  case class People(name: String, contactInfo: ContactInfo)
  val mike = People("Mike", ContactInfo("202-295-3928", Address("1 Main ST", "Chicago", "IL")))

  //existing Show instance for Address
  implicit val addressShow: Show[Address] = { a =>
    s"${a.street}, ${a.city}, ${a.state}"
  }

  implicit val peopleShow: Show[People] = {
    semi.show
  } //auto derive Show for People

  println(mike.show)
}
