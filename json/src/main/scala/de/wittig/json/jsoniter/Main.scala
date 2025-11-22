package de.wittig.json.jsoniter

import java.time.{Year, YearMonth}
import scala.concurrent.duration.DurationInt
import com.github.plokhotnyuk.jsoniter_scala.core.*

@main
def main(): Unit =

  val p = Person("lkj", 234, 5.seconds, Year.now, YearMonth.now, List(1, 2), Car("Astra", "Opel", 2024))
  val s = """{
            |"name":"lkj",
            |"alter":234,
            |"dauer":{"length":5,"unit":"SECONDS"},
            |"year":"2024",
            |"yearMonth":"2024-12",
            |"numbers":[1,2],
            |"car":{"brand":"Astra","model":"Opel","baujahr":2024}
            |}""".stripMargin

  println(writeToString(p))
  println(readFromString[Person](s))

  // Mit Extension Method
  println(p.toJson)
  println(s.fromJson[Person])

extension (payload: String)
  def fromJson[T: JsonValueCodec]: T = readFromString(payload)

extension [T: JsonValueCodec](obj: T)
  def toJson: String = writeToString(obj)
