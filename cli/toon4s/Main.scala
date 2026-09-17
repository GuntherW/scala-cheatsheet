//> using dep "io.github.riccardomerolla::zio-toon::0.2.5"

import io.github.riccardomerolla.ziotoon._
import ToonValue._

@main
def main() =
  // Create a ToonValue
  val person = obj(
    "name" -> str("Alice"),
    "age" -> num(30),
    "active" -> bool(true)
  )

  // Pure encoding - returns String
  val toonString: String = Toon.encode(person)
  println(toonString)
  // Output:
  // name: Alice
  // age: 30
  // active: true

  // Pure decoding - returns Either[ToonError, ToonValue]
  val result: Either[ToonError, ToonValue] = Toon.decode(toonString)
  result match {
    case Right(value) => println(s"Decoded: $value")
    case Left(error)  => println(s"Error: ${error}")
  }
