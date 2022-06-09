package de.codecentric.wittig.scala.enums

import de.codecentric.wittig.scala.enums.Main.Color

object Main extends App:

  enum Color(val rgb: Int):
    case Red   extends Color(0xff0000)
    case Green extends Color(0x00ff00)
    case Blue  extends Color(0x0000ff)

  assert(Color.values sameElements Array(Color.Red, Color.Green, Color.Blue))

  assert(Array(1, 2, 3, 4, 5, 6) == Array(1, 2, 3, 4, 5, 6))