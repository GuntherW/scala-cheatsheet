package de.wittig.json.borer

import io.bullet.borer.{Cbor, Codec, Decoder, Encoder}

import java.time.LocalDate
import io.bullet.borer.derivation.MapBasedCodecs.*

@main
def caseClass(): Unit =

  case class Person(name: String, age: Int, date: LocalDate = LocalDate.now) derives Codec
  object Person:
    given Encoder[LocalDate] = Encoder.forLong.contramap[LocalDate](_.toEpochDay)
    given Decoder[LocalDate] = Decoder.forLong.map(LocalDate.ofEpochDay)

  val p = Person("name", 123)

  val bytes: Array[Byte] = Cbor.encode(p).toByteArray // throws on error
  println(bytes.mkString)

  val list: Person = Cbor.decode(bytes).to[Person].value // throws on error
  println(list)
