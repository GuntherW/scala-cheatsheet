package de.codecentric.wittig.scala.aoc

import scala.io.Source

extension (s: String)
  def getLines: List[String] = Source.fromResource(s).getLines().toList
