package de.codecentric.wittig.scala.zio

import zio.DefaultRuntime
import zio.console._

// using scala.App
object MainPlain extends App {
  val runtime = new DefaultRuntime {}
  runtime.unsafeRun(putStrLn("Hallo Welt"))
}
