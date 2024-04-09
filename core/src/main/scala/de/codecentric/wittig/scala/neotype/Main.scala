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

  case class Person(name: String, age: Int)

  type ElderlyPerson = ElderlyPerson.Type
  object ElderlyPerson extends Newtype[Person]:
    override inline def validate(value: Person): Boolean =
      value.age > 65

  val elder = ElderlyPerson(Person("Lazarus", 70))
//  val youth = ElderlyPerson(Person("Kit", 30))
