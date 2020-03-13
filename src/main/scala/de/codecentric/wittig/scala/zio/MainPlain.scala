package de.codecentric.wittig.scala.zio

import zio.{Runtime, Task}

// using scala.App
object MainPlain extends App {
  val runtime = Runtime.default
  runtime.unsafeRun(Task("Hallo Welt"))
}
