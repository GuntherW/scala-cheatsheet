package de.codecentric.wittig.scala.cats.effects

import cats.effect.IO

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends App {

  val ioa = IO { println("hey!") }

  val program: IO[Unit] =
    for {
      _ <- ioa
      _ <- ioa
    } yield ()

  program.unsafeRunSync()
  val program1 = fib(6)
  program1.unsafeRunAsync(a => println(s"fib $a"))

  def fib(n: Int, a: Long = 0, b: Long = 1): IO[Long] =
    IO(a + b).flatMap { b2 =>
      if (n > 0)
        fib(n - 1, b, b2)
      else
        IO.pure(b2)
    }

  //val f1: IO[Int] = IO.fromFuture(IO(Future(1)))
  val f1: IO[Int] = IO.fromFuture(IO(Future.failed(new Exception("lkk"))))

  def inc(i: Int) = i + 1

  f1.map(inc).unsafeRunAsync {
    case Left(t)  => println(s"Fehler $t:")
    case Right(i) => println(s"Erg: $i")
  }
}
