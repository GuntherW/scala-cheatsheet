package de.codecentric.wittig.scala.java.time
import java.time.*

@main
def main(): Unit =

  val berlinZone: ZoneId = ZoneId.of("Europe/Berlin")
  val utc: ZoneId        = ZoneId.of("UTC")

  val i             = Instant.now
  val zonedDateTime = i.atZone(berlinZone)
  val p             = zonedDateTime.withZoneSameInstant(berlinZone)

  println(i)
  println(zonedDateTime)
  println(p)
  println(p.toInstant)

  val pastDate  = ZonedDateTime.of(1500, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant
  val pastDate2 = LocalDate.of(1500, 1, 1).atStartOfDay(berlinZone).toInstant
  val pastDate3 = LocalDate.of(1500, 1, 1).atStartOfDay(utc).toInstant
  val pastDate4 = LocalDate.of(1500, 1, 1).atStartOfDay.atZone(utc)
  val pastDate5 = LocalDate.of(1500, 1, 1).atStartOfDay.toInstant(ZoneOffset.UTC)
  println(pastDate)
  println(pastDate2)
  println(pastDate3)
  println(pastDate4)
  println(pastDate5)
