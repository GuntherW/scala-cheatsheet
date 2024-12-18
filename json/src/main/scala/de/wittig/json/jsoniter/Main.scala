package de.wittig.json.jsoniter

import java.time.{Year, YearMonth}
import scala.concurrent.duration.DurationInt
import com.github.plokhotnyuk.jsoniter_scala.core._

object Main extends App:

  val p = Person("lkj", 234, 5.seconds, Year.now, YearMonth.now, List(1, 2))
  println(writeToString(p))

  val p2 = readFromString[Person]("""{"name":"lkj","alter":234,"dauer":{"length":5,"unit":"SECONDS"},"year":"2024","yearMonth":"2024-12","numbers":[1,2]}""")
  println(p2)
