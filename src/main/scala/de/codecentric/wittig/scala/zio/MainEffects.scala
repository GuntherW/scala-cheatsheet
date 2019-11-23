package de.codecentric.wittig.scala.zio

import cats.implicits._
import zio.clock.Clock
import zio.duration._
import zio.{App, IO, Task, UIO, ZIO}
import zio.console._
import scala.concurrent.Future
import scala.io.StdIn
import scala.util.Try

object MainEffects extends App {
  def run(args: List[String]) =
    fib(100).fork
      .map(i => println(s"Hallo $i"))
      .map(_ => 1)

  val zoption: IO[Unit, Int]          = ZIO.fromOption(Some(2))
  val zoption2: ZIO[Any, String, Int] = zoption.mapError(_ => "It wasn't there!")
  val zeither: IO[Nothing, String]    = ZIO.fromEither(Right("Success!"))
  val ztry: Task[Int]                 = ZIO.fromTry(Try(42 / 0))
  val zfun: ZIO[Int, Nothing, Int]    = ZIO.fromFunction((i: Int) => i * i)

  lazy val future = Future.successful("Hello!")
  val zfuture: Task[String] = ZIO.fromFuture { implicit ec =>
    future.map(_ => "Goodbye!")
  }

  // either and absolve
  val zeither1: ZIO[Any, String, Int]            = IO.fail("Uh oh!")
  val z1: ZIO[Any, Nothing, Either[String, Int]] = zeither1.either
  val z2: ZIO[Any, String, Int]                  = z1.absolve

  val sleep: ZIO[Clock, Nothing, Unit] = ZIO.sleep(5.seconds)

  def fib(n: Long): UIO[Long] =
    UIO {
      if (n <= 1) UIO.succeed(n)
      else fib(n - 1).zipWith(fib(n - 2))(_ + _)
    }.flatten
}
