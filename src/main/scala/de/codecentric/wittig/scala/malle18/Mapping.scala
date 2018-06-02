package de.codecentric.wittig.scala.malle18

import io.circe.Decoder.Result
import io.circe.generic.auto._
import io.circe.jawn.parse
import io.circe.{Decoder, Json, ParsingFailure}
import shapeless.LabelledGeneric
import shapeless.syntax.singleton._

final case class Person(firstname: String, lastname: String, age: Int)

object Person {
  implicit val personGen = LabelledGeneric[Person]
}

// def foo[A, B](implicit LabelledRecord[A], FromLabelledRecord[B]): B

final case class PersonDto(name: String, age: Long)

object MappingGen {

  val personMapping = {
    // name = lastname + ", " + firstname
    // age = age.toLong

    // lastname = name.split(", ").head
    // firstname = name.split(", ").last
    // age = age.toInt
  }

  def convert[A, B: Decoder](mapping: A => Json)(input: A): Either[ParsingFailure, Result[B]] = {
    parse(mapping(input).toString).map(_.as[B])
  }
}

object MappingSimple {
  def personConverter(input: Person): PersonDto = {
    PersonDto(s"${input.lastname}, ${input.firstname}", input.age)
  }
}

object Examples extends App {
  val fooBar = Person("Foo", "Bar", 42)

  println(MappingSimple.personConverter(fooBar))

  def personMapping(person: Person): Json = {
    Json.obj("name" -> Json.fromString(s"${person.lastname}, ${person.firstname}"), "age" -> Json.fromInt(person.age))
  }

  println(MappingGen.convert[Person, PersonDto](personMapping)(fooBar))
}

object Mapping2 extends App {

  def convert(person: Person) = {
    val personGen = implicitly[LabelledGeneric[Person]]

    val rec: personGen.Repr = personGen.to(person)

    val result = rec + ('firstname ->> "fjdskl")

    println(result)
  }

  convert(Examples.fooBar)
}
