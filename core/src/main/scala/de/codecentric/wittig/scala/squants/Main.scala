package de.codecentric.wittig.scala.squants

import squants.*
import squants.energy.Megawatts

object Main extends App {

  import squants.time.{Days, Hours}
  val ratio = Days(1) / Hours(3)
  println(ratio)

  import squants.energy.Kilowatts
  val load       = Kilowatts(1.2)
  val time       = Hours(2)
  val energyUsed = load * time
  println(energyUsed)
  println(load in Megawatts)
  println(load to Megawatts)

  import squants.market.EUR
  val tenEuro = EUR(10)

  println(tenEuro + EUR(0.3))
}
