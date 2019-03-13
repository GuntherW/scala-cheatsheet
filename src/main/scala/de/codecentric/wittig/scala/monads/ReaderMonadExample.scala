package de.codecentric.wittig.scala.monads
import cats.data.Reader

object ReaderMonadExample extends App {

  def hallo1: Reader[Config, String] = Reader(f => s"hallo1 ${f.x}")
  def hallo2: Reader[Config, String] = Reader(f => s"hallo2 ${f.y}")

  def program: Reader[Config, (String, String)] =
    for {
      h1 <- hallo1
      h2 <- hallo2
    } yield (h1, h2)

  println(program.run(Config("x", "y")))
}

case class Config(x: String, y: String)
