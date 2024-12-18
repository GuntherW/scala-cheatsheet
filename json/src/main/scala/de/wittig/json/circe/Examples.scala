package de.wittig.json.circe

import io.circe.syntax.EncoderOps

import java.time.{Year, YearMonth}
import scala.concurrent.duration.DurationInt

object Examples extends App:

  val p = Person("lkj", 234, 5.seconds, Year.now, YearMonth.now, List(1, 2))
  println(p.asJson.toString)
