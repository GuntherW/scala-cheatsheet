package de.wittig.json.circe

import io.circe.{Decoder, Encoder, Json, KeyEncoder}

import java.time.{Year, YearMonth}
import scala.concurrent.duration.{DurationLong, FiniteDuration}

case class Person(
    name: String,
    alter: Int,
    dauer: FiniteDuration,
    year: Year,
    yearMonth: YearMonth,
    numbers: List[Int]
) derives Encoder, Decoder

object Person:
  given yearKeyEncoder: KeyEncoder[Year] = (year: Year) => year.toString + "lkj"

  given Encoder[FiniteDuration] = Encoder.instance { dur =>
    Json.fromBigDecimal(BigDecimal(dur.toSeconds) / 60)
  }

  given Decoder[FiniteDuration] = Decoder.instance { c =>
    c.as[BigDecimal].map(bd => (bd * 60).toLong.seconds)
  }
