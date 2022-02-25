package de.codecentric.wittig.scala.circe

import io.circe.parser.*
import io.circe.{Json, ParsingFailure}

object Parsing extends App:
  val rawJson: String =
    """
      {
        "foo": "bar",
        "baz": 123,
        "list of stuff": [ 4, 5, 6 ]
      }
    """

  val parseResult: Either[ParsingFailure, Json] = parse(rawJson) // Right
  println(parseResult)

  val badResult = parse("falsch") // Left(ParsingFailure)
  println(badResult)

  // use with getOrElse
  val json: Json = parse(rawJson).getOrElse(Json.Null)
  println(json)
