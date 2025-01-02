package de.codecentric.wittig.scala.squants

import squants.*
import squants.energy.Megawatts
import squants.time.{Days, Hours}
import squants.energy.Kilowatts
import squants.market.EUR

object Main extends App {

  val ratio = Days(1) / Hours(3)
  println(ratio)

  val load       = Kilowatts(1.2)
  val time       = Hours(2)
  val energyUsed = load * time
  println(energyUsed)
  println(load in Megawatts)
  println(load to Megawatts)

  val tenEuro = EUR(10)

  println(tenEuro + EUR(0.3))
}
