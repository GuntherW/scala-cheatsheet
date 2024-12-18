package de.wittig.json.upicklelib

import upickle.default.*
import java.time.{Year, YearMonth}
import scala.concurrent.duration.FiniteDuration

case class Person(
    name: String,
    alter: Int,
    dauer: FiniteDuration,
    year: Year,
    yearMonth: YearMonth,
    numbers: List[Int]
) derives ReadWriter

object Person:
  given ReadWriter[Year]      = readwriter[Int].bimap[Year](_.getValue, Year.of)
  given ReadWriter[YearMonth] = readwriter[String].bimap[YearMonth](_.toString, YearMonth.parse)
