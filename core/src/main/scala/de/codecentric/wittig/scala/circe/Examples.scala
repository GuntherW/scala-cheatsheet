package de.codecentric.wittig.scala.circe

import java.time.{Year, YearMonth}

import scala.concurrent.duration.{DurationInt, DurationLong, FiniteDuration}

import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json, KeyEncoder}

object Examples extends App:

  case class Person(
      name: String,
      alter: Int,
      dauer: FiniteDuration,
      year: Year,
      yearMonth: YearMonth,
  ) derives Encoder, Decoder

  object Person:
    given yearKeyEncoder: KeyEncoder[Year] = (year: Year) => year.toString + "lkj"

    given Encoder[FiniteDuration] = Encoder.instance { dur =>
      Json.fromBigDecimal(BigDecimal(dur.toSeconds) / 60)
    }

    given Decoder[FiniteDuration] = Decoder.instance { c =>
      c.as[BigDecimal].map(bd => (bd * 60).toLong.seconds)
    }

  val p = Person("lkj", 234, 5.seconds, Year.now, YearMonth.now)

  println(p.asJson.toString)
