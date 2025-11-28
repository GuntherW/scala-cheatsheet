package de.wittig.macros.inlines

inline def test(x: String | Int): String =
  inline x match
    case _: String => "s"
    case _: Int    => "i"

@main
def matcher(): Unit =
  println(test("s")) // will compile to println("s")
  println(test(1)) // will compile to println("i")
