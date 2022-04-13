package de.wittig.zio1

import zio.Console.*
import zio.ZIOAppDefault

object Main extends ZIOAppDefault:

  def run = myAppLogic

  private val myAppLogic =
    for
      _ <- printLine("Hello! What is your name?")
      n <- readLine
      _ <- printLine(s"Hello, $n, good to meet you!")
    yield ()
