package de.wittig.json.circe

import de.wittig.json.circe.WithConfiguration.Foo.given_Configuration
import io.circe.Decoder
import io.circe.derivation.*
import io.circe.parser.*
import io.circe.syntax.*

object WithConfigurationDeriving extends App:

  case class Address(cityName: String = "Kölle") derives ConfiguredCodec
  case class Person(givenName: String, age: Int = 123, recentAddress: Address = Address()) derives ConfiguredCodec
  object Person:
    given Configuration = Configuration.default.withDefaults.withKebabCaseMemberNames

  println(decode[Person]("""{ "given-name": "Tom" }"""))
  println(decode[Person]("""{ "given-name": "Tim", "age": 1 }"""))
  println(decode[Person]("""{ "given-name": "Tim", "age": 1, "recent-address": { "city-name": "Bonn" } }"""))

  println(Person("Peter").asJson)
