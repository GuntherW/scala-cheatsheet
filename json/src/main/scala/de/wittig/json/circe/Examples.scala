package de.wittig.json.circe

import io.circe.syntax.EncoderOps

import java.time.{Year, YearMonth}
import scala.concurrent.duration.DurationInt
import scala.util.chaining.scalaUtilChainingOps

@main
def examples(): Unit =

  Person("lkj", 234, 5.seconds, Year.now, YearMonth.now, List(1, 2))
    .asJson
    .tap(println)
