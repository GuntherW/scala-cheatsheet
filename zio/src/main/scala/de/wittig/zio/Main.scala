package de.wittig.zio

import zio.Console.*
import zio.App

object Main extends App {

  def run(args: List[String]) =
    myAppLogic.exitCode

  private val myAppLogic =
    for
      _ <- printLine("Hello! What is your name?")
      n <- readLine
      _ <- printLine(s"Hello, $n, good to meet you!")
    yield ()
}
