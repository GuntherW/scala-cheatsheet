package de.wittig.json.jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

import java.time.{Year, YearMonth}
import scala.concurrent.duration.FiniteDuration

case class Person(
    name: String,
    alter: Int,
    dauer: FiniteDuration,
    year: Year,
    yearMonth: YearMonth,
    numbers: List[Int],
    car: Car
)

object Person:
  given userCodec: JsonValueCodec[Person] = JsonCodecMaker.make

case class Car(brand: String, model: String, baujahr: Int)
