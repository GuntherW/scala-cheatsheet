package de.codecentric.wittig.scala.neotype
import io.circe.Codec
import io.circe.derivation.Configuration
import neotype.*

object Main extends App:

  type NonEmptyString = NonEmptyString.Type
  object NonEmptyString extends Newtype[String]:
    override inline def validate(input: String): Boolean = input.nonEmpty

  object StringStartsWithNumber extends Newtype[String]:
    override inline def validate(input: String): Boolean = """^\d.*""".r.matches(input)

  NonEmptyString("Hallo Welt")
  StringStartsWithNumber("123")
  private val abc = "abc"
  println(StringStartsWithNumber.make(abc))

  case class Person(name: NonEmptyString, age: Int) // derives Codec.AsObject
