package de.codecentric.wittig.scala.circe

import io.circe.Decoder.decodeLocalDateTimeWithFormatter
import io.circe.parser.decode
import io.circe.parser.decode
import io.circe.syntax._

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, OffsetDateTime, ZonedDateTime}

object DecodingSimple extends App {

  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'Z")
  val ziel      = "\"2017-05-22T09:00:00.000Z+0200\""

  println(LocalDateTime.now.asJson)
  println(decode[LocalDateTime]("\"2017-05-22T09:00:00.000\""))
  implicit val localDateTimeDecoder = decodeLocalDateTimeWithFormatter(formatter)
  println(decode[LocalDateTime](ziel))

  println(Instant.now.asJson)
  println(decode[Instant]("\"2017-05-22T09:00:00.000Z\""))

  println(ZonedDateTime.now.asJson)
  println(decode[ZonedDateTime]("\"2020-12-18T12:52:03.068+01:00[Europe/Berlin]\""))

  println(OffsetDateTime.now.asJson)
  println(decode[OffsetDateTime]("\"2020-12-18T12:52:03.068+01:00\""))

}
