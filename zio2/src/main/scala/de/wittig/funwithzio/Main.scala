package de.wittig.funwithzio

import zio.*
import zio.stream.*
import zio.Console.*

object Main extends ZIOAppDefault:

  private val program = printLine("Guten") *> printLine("Tag")

  def run = program
