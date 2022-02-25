package de.wittig.zio

import zio.Runtime
import zio.Console.*

// using scala.App
object MainPlain extends App:
  val runtime = Runtime.default

  runtime.unsafeRun(printLine("Hallo Welt"))
