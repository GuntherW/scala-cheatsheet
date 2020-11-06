package de.wittig.zio

import zio.Runtime
import zio.console.putStrLn

// using scala.App
object MainPlain extends App {
  val runtime = Runtime.default

  runtime.unsafeRun(putStrLn("Hallo Welt"))
}
