package de.codecentric.wittig.scala.streams

import cats.effect.{IO, IOApp}
import fs2.{text, Pipe, Stream}
import fs2.compression.Compression
import fs2.io.file.{Files, Path}

@main
def main(): Unit =

  Stream(1, 2, 3)
    .toList
    .foreach(println)

object MainIO extends IOApp.Simple:
  val run: IO[Unit] = Stream
    .duration[IO]
    .evalMap(d => IO(println(d)))
    .take(10)
    .compile
    .drain

object Converter extends IOApp.Simple:

  val converter: fs2.Stream[IO, Unit] =
    def fahrenheitToCelsius(f: Double): Double = (f - 32.0) * (5.0 / 9.0)

    Files[IO].readAll(Path("fahrenheit.txt"))
      .through(text.utf8.decode)
      .through(text.lines)
      .filter(s => s.trim.nonEmpty && !s.startsWith("//"))
      .map(line => fahrenheitToCelsius(line.toDouble).toString)
      .intersperse("\n")
      .through(text.utf8.encode)
      .through(Files[IO].writeAll(Path("celsius.txt")))

  def run: IO[Unit] = converter.compile.drain

object ConverterAndZipper extends IOApp.Simple:

  private def fahrenheitToCelsius(f: Double): Double = (f - 32.0) * (5.0 / 9.0)

  private def writeToFile(fileName: String): Pipe[IO, Byte, Unit] =
    _.through(Files[IO].writeAll(Path(fileName)))

  private def zipToFile(zipName: String): Pipe[IO, Byte, Unit] =
    _.through(Compression[IO].gzip())
      .through(Files[IO].writeAll(Path(zipName)))

  private def zipToFileWithCustomInnerName(zipName: String): Pipe[IO, Byte, Unit] =
    _.through(Compression[IO].gzip(fileName = Some("anders.txt"), comment = Some("Was zum Teufel"))) // geht nicht. Bug?
      .through(Files[IO].writeAll(Path(zipName)))

  def run: IO[Unit] = Files[IO].readAll(Path("fahrenheit.txt"))
    .through(text.utf8.decode)
    .through(text.lines)
    .filter(s => s.trim.nonEmpty && !s.startsWith("//"))
    .map(line => fahrenheitToCelsius(line.toDouble).toString)
    .intersperse("\n")
    .through(text.utf8.encode)
    .broadcastThrough( // Fan out (broadcast)
      writeToFile("celsius.txt"),
      zipToFile("celsius.txt.zip"),
      zipToFileWithCustomInnerName("celsius.zip")
    )
    .compile.drain
