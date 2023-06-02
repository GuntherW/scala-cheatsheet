package de.codecentric.wittig.scala.enums

import de.codecentric.wittig.scala.enums.Main.Color

object Main extends App:

  enum Color(val value: Int):
    case Red   extends Color(0xff0000)
    case Green extends Color(0x00ff00)
    case Blue  extends Color(0x0000ff)

  object Color:
    def apply(value: Int): Option[Color] = values.find(_.value == value)

  assert(Color.values sameElements Array(Color.Red, Color.Green, Color.Blue))

  assert(Array(1, 2, 3, 4, 5, 6) != Array(1, 2, 3, 4, 5, 6))

  assert(Color.fromOrdinal(2) == Color.Blue)
  assert(Color.valueOf("Red") == Color.Red)
  assert(Color.values.length == 3)
  assert(Color.values.indexOf(Color.Green) == 1)
  assert(Color.Green.ordinal == 1)
  assert(Color(0x0000ff) == Some(Color.Blue))
