package de.codecentric.wittig.scala.fs2

import cats.effect._
import cats.data._
import cats.implicits._
import fs2._

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    IO(println("Hallo Welt")) *> IO(ExitCode.Success)

  import fs2._
  // import fs2._

  def tk[F[_], O](n: Long): Pipe[F, O, O] =
    in =>
      in.scanChunksOpt(n) { n =>
        if (n <= 0) None
        else
          Some(c =>
            c.size match {
              case m if m < n => (n - m, c)
              case m          => (0, c.take(n.toInt))
          })
    }
  // tk: [F[_], O](n: Long)fs2.Pipe[F,O,O]

  Stream(1, 2, 3, 4).through(tk(2)).toList
}
