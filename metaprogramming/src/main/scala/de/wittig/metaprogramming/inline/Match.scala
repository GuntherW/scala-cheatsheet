package de.wittig.metaprogramming.inline

import scala.util.Random

inline def test(x: String | Int): String =
  inline x match
    case _: String => "s"
    case _: Int    => "i"

object Match extends App:
  println(test("s")) // will compile to println("s")
  println(test(1))   // will compile to println("i")
