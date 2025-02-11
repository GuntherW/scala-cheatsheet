package de.wittig.json.borer

import io.bullet.borer.Cbor
import io.circe.{Decoder, Encoder}
import io.bullet.borer.compat.circe.*

import java.time.LocalDate

object CaseClassWithCirce extends App:

  case class Person(name: String, age: Int, date: LocalDate = LocalDate.now) derives Encoder, Decoder

  val p = Person("name", 123)

  val bytes: Array[Byte] = Cbor.encode(p).toByteArray // throws on error
  println(bytes.mkString)

  val list: Person = Cbor.decode(bytes).to[Person].value // throws on error
  println(list)
