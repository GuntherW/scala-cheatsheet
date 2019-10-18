package de.codecentric.wittig.scala.zio

import zio.DefaultRuntime
import zio.console._

object MainPlain extends App {

  val runtime = new DefaultRuntime {}
  runtime.unsafeRun(putStrLn("Hallo Welt"))
}
