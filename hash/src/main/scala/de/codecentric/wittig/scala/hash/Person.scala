package de.codecentric.wittig.scala.hash

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

case class Address(street: String, city: String)
case class Person(name: String, age: Int, address: Address, hobbies: List[String] = Nil)

object Address:
  given JsonValueCodec[Address] = JsonCodecMaker.make

object Person:
  given JsonValueCodec[Person] = JsonCodecMaker.make
