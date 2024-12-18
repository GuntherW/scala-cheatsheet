package de.wittig.json.ziojsonlib

import java.time.{Year, YearMonth}
import scala.concurrent.duration.{Duration, FiniteDuration}
import zio.json.*

case class Person(
    name: String,
    alter: Int,
    dauer: FiniteDuration,
    year: Year,
    yearMonth: YearMonth,
    numbers: List[Int]
)

object Person:
  given JsonDecoder[FiniteDuration] =
    JsonDecoder.string.mapOrFail { str =>
      try
        // Attempt to parse the duration string
        Duration(str) match {
          case d: FiniteDuration => Right(d)
          case _                 => Left(s"Non-finite duration: $str")
        }
      catch {
        case ex: Exception => Left(s"Invalid duration format: ${ex.getMessage}")
      }
    }

  given JsonDecoder[Person] = DeriveJsonDecoder.gen[Person]

  given JsonEncoder[FiniteDuration] = JsonEncoder.string.contramap(_.toString)
  given JsonEncoder[Person]         = DeriveJsonEncoder.gen[Person]
