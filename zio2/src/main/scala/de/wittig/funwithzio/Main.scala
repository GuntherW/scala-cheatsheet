package de.wittig.funwithzio

import zio.*
import zio.stream.*
import zio.Console.*

object Main extends ZIOAppDefault {

  def run = printLine("lkj") *> printLine("lkjlkj")
}
