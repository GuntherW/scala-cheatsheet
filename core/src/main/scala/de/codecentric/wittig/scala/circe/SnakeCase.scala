package de.codecentric.wittig.scala.circe

import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec}
import io.circe.parser._
import JsonConfiguration._

/** Beispiel, wie von einem snake_case Json zu einer Case Class in CamelCase gemappt/geparst werden kann.
  */
object JsonConfiguration {
  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames
}

@ConfiguredJsonCodec
final case class Data(fullName: String, ageInYears: Int)

object SnakeCase extends App {
  val snakeCaseString = """{
                     |  "full_name" : "Alice",
                     |  "age_in_years" : 27
                     |}""".stripMargin

  println(decode[Data](snakeCaseString))
}
