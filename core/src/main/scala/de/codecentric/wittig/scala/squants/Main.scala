package de.codecentric.wittig.scala.squants

import squants.energy.Megawatts
import squants.time.{Days, Hours}
import squants.energy.Kilowatts
import squants.market.EUR

@main
def main(): Unit =

  val ratio = Days(1) / Hours(3)
  val load  = Kilowatts(1.2)
  val time  = Hours(2)

  println(ratio)
  println(load)
  println(load * time)
  println((load * time) / Hours(4))
  println(load in Megawatts)
  println(load to Megawatts)
  println(EUR(10) + EUR(0.3))
