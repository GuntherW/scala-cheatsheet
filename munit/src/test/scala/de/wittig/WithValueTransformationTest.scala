package de.wittig

import io.circe.generic.semiauto.deriveCodec
import io.circe.syntax.EncoderOps
import munit.FunSuite
import cats.syntax.either.*
import io.circe.*, io.circe.parser.*

class WithValueTransformationTest extends FunSuite {

  case class Person(name: String, age: Int, hobbies: List[Hobby])
  case class Hobby(name: String, cost: Option[Int] = None)

  given Codec.AsObject[Hobby]  = deriveCodec[Hobby]
  given Codec.AsObject[Person] = deriveCodec[Person]

  test("test") {
    val p          = Person("hans", 22, List(Hobby("tanzen")))
    val jsonString =
      """{
        |  "name" : "hans",
        |  "age" : 22,
        |  "hobbies" : [
        |    {
        |      "name" : "tanzen",
        |      "cost" : null
        |  }
        |  ]
        |}""".stripMargin
    assertEquals(p.asJson, parse(jsonString).getOrElse(Json.Null))
  }
}
