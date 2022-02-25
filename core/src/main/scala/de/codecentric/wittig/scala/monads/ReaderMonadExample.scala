package de.codecentric.wittig.scala.monads
import cats.data.Reader
import cats.*

object ReaderMonadExample extends App:
  case class Config(x: String, y: String)

  def hallo1: Reader[Config, String] = Reader(config => s"hallo1 ${config.x}")
  def hallo2: Reader[Config, String] = Reader(config => s"hallo2 ${config.y}")

  def program: Reader[Config, (String, String)] =
    for
      h1 <- hallo1
      h2 <- hallo2
    yield (h1, h2)

  println(program.run(Config("x-wert", "y-wert")))
