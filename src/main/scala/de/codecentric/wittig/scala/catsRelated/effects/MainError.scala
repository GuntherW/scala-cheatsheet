package de.codecentric.wittig.scala.catsRelated.effects

import cats.effect._
import cats.implicits._

import scala.util.control.NoStackTrace

sealed trait BusinessError extends NoStackTrace
case object RandomError    extends BusinessError

object MainError extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    val io1          = IO(1)
    val io2: IO[Int] = IO.raiseError(new IllegalArgumentException("ll"))
    val io3: IO[Int] = IO.raiseError(new IllegalArgumentException("kk"))

    val erg = for {
      e1 <- io1
      e2 <- io2
      e3 <- io3
    } yield e1 + e2 + e3

    val erg2 = (io1, io2, io3).mapN {
      case (eins, zwei, drei) => eins + zwei + drei
    }

    erg
      .handleError { t =>
        println(t)
        println("+" * 50)
        123
      }
      .map { i =>
        println(i)
        ExitCode.Success
      }
    erg2
      .handleError { t =>
        println(t)
        println("+" * 50)
        123
      }
      .map { i =>
        println(i)
        ExitCode.Success
      }
  }
}
