package de.codecentric.wittig.scala.streams

import cats.effect.{IO, IOApp}
import fs2.{text, Stream}
import fs2.io.file.{Files, Path}

object Main extends App {

  Stream(1, 2, 3)
    .toList
    .foreach(println)

}

object MainIO extends IOApp.Simple {
  val run = Stream
    .duration[IO]
    .evalMap(d => IO(println(d)))
    .take(10)
    .compile
    .drain
}

object Converter extends IOApp.Simple {

  val converter: fs2.Stream[IO, Unit] = {
    def fahrenheitToCelsius(f: Double): Double = (f - 32.0) * (5.0 / 9.0)

    Files[IO].readAll(Path("fahrenheit.txt"))
      .through(text.utf8.decode)
      .through(text.lines)
      .filter(s => s.trim.nonEmpty && !s.startsWith("//"))
      .map(line => fahrenheitToCelsius(line.toDouble).toString)
      .intersperse("\n")
      .through(text.utf8.encode)
      .through(Files[IO].writeAll(Path("celsius.txt")))
  }

  def run: IO[Unit] = converter.compile.drain
}
