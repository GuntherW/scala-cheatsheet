import zio.json.JsonCodec
import zio.schema.codec.JsonCodec.jsonCodec
import zio.schema.{DeriveSchema, Schema}
import scala.util.chaining.*

import java.time.LocalDate

object JsonDtoToDomainObjectExample extends App:

  case class PersonDto(firstName: String, lastName: String, birthday: (Int, Int, Int))

  object PersonDto {
    given schema: Schema[PersonDto]   = DeriveSchema.gen[PersonDto]
    given codec: JsonCodec[PersonDto] = jsonCodec[PersonDto](schema)
  }

  case class Person(name: String, birthdate: LocalDate)

  object Person {
    given schema: Schema[Person]   = DeriveSchema.gen[Person]
    given codec: JsonCodec[Person] = jsonCodec[Person](schema)

    val personDtoMapperSchema: Schema[Person]       = PersonDto.schema.transform(
      f = personDto => {
        val (year, month, day) = personDto.birthday
        Person(
          personDto.firstName + " " + personDto.lastName,
          birthdate = LocalDate.of(year, month, day)
        )
      },
      g = person => {
        val fullNameArray = person.name.split(" ")
        PersonDto(
          fullNameArray.head,
          fullNameArray.last,
          (
            person.birthdate.getYear,
            person.birthdate.getMonthValue,
            person.birthdate.getDayOfMonth
          )
        )
      }
    )
    val personDtoJsonMapperCodec: JsonCodec[Person] = jsonCodec[Person](personDtoMapperSchema)
  }

  val json: String =
    """
      |{
      |   "firstName": "John",
      |   "lastName": "Doe",
      |   "birthday": [[1981, 07], 13]
      |}
      |""".stripMargin

  // I can decode the json to a PersonDto
  PersonDto.codec.decodeJson(json).tap(println)

  // I can decode the json to a Person directly, too
  Person.personDtoJsonMapperCodec.decodeJson(json).tap(println)
