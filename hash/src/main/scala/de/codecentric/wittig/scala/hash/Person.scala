package de.codecentric.wittig.scala.hash

import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.EncoderOps

case class Person(name: String, age: Int, address: Address, hobbies: List[String] = Nil)
case class Address(street: String, city: String) derives Encoder, Decoder

object Person:

  given Encoder[Person] = (p: Person) =>
    Json.obj(
      ("name", Json.fromString(p.name)),
      ("age", Json.fromInt(p.age)),
      ("address", p.address.asJson),
      ("hobbies", Json.fromValues(p.hobbies.sorted.map(Json.fromString)))
    )

  given Decoder[Person] = deriveDecoder[Person].map: p =>
    p.copy(hobbies = p.hobbies.sorted)
