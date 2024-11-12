package de.wittig.zio

import zio.*
import zio.Console.*

object Main extends ZIOAppDefault:

  private val program = printLine("Guten") *> printLine("Tag")

  def run = program
