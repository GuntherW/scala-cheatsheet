package de.codecentric.wittig.scala.timesheetservice
import cats.implicits.*

object Main extends App {

  println("Hallo Welt")
  val a: Option[(String, String)] = Some(("lkj", "lkj"))

  println(a.exists(_._2 === "lkj"))
}
