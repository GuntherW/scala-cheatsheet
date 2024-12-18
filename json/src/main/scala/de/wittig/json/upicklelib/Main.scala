package de.wittig.json.upicklelib

import upickle.default.*

import java.time.{Year, YearMonth}
import scala.concurrent.duration.DurationInt

object Main extends App:

  val p = Person("lkj", 234, 5.seconds, Year.now, YearMonth.now, List(1, 2))
  println(write(p))

  val p2 = read[Person]("""{"name":"lkj","alter":234,"dauer":"5000000000","year":2024,"yearMonth":"2024-12","numbers":[1,2]}""")
  println(p2)
