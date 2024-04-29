package de.codecentric.wittig.scala.circe

import io.circe.derivation.Configuration
import io.circe.generic.semiauto.*
import io.circe.parser.*
import io.circe.derivation.*
import io.circe.syntax.*
import io.circe.{Decoder, Json, ParsingFailure}
import de.codecentric.wittig.scala.circe.WithConfiguration.Foo.given_Configuration

object WithConfigurationDeriving extends App:

  case class Address(cityName: String = "Kölle") derives ConfiguredCodec
  case class Person(givenName: String, age: Int = 123, recentAddress: Address = Address()) derives ConfiguredCodec
  object Person:
    given Configuration = Configuration.default.withDefaults.withKebabCaseMemberNames

  println(decode[Person]("""{ "given-name": "Tom" }"""))
  println(decode[Person]("""{ "given-name": "Tim", "age": 1 }"""))
  println(decode[Person]("""{ "given-name": "Tim", "age": 1, "recent-address": { "city-name": "Bonn" } }"""))

  println(Person("Peter").asJson)
