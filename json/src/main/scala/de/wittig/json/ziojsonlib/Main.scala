package de.wittig.json.ziojsonlib
import zio.json.*

import java.time.{Year, YearMonth}
import scala.concurrent.duration.DurationInt

@main
def main(): Unit =

  val p = Person("lkj", 234, 5.seconds, Year.now, YearMonth.now, List(1, 2))
  println(p.toJson)

  val p2 = """{"name":"lkj","alter":234,"dauer":"5 seconds","year":"2024","yearMonth":"2024-12","numbers":[1,2]}""".fromJson[Person]
  println(p2)
