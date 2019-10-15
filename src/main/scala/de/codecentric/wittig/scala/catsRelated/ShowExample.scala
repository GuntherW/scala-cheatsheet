package de.codecentric.wittig.scala.catsRelated
import cats.Show
import cats.derived.{auto, semi}
import cats.syntax.show.toShow

import scala.concurrent.duration._

case class Container(name: String, duration: FiniteDuration)

object ShowExample extends App {

//  implicit val showFin: Show[FiniteDuration] = Show.show(round)
  implicit val showPerson: Show[Container] = {
    import cats.implicits._
    import auto.show._
    semi.show
  }

  val john = Container("John", 31 minutes)

  println(john.show)
  println(john)

  def round(f: FiniteDuration): String =
    BigDecimal(f.toUnit(HOURS)).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble + "h"
}
