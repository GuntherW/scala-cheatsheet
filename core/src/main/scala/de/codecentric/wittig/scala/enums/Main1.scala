package de.codecentric.wittig.scala.enums

@main
def main1: Unit =

  enum MeinEnum:
    case A(i: Int)
    case B(s: String, d: Double)
    case C

  println(MeinEnum.A(3))
  println(MeinEnum.B("s", 5))
