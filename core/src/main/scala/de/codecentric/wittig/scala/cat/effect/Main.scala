package de.codecentric.wittig.scala.cat.effect

import cats.effect.{IO, IOApp}

import scala.concurrent.duration.*
import scala.util.Random

object Main extends IOApp.Simple {

  private val delayedPrint = IO.sleep(1.second) *> IO(println(Random.nextInt(100)))

  private val programm1 = // sequentiell
    for
      _ <- delayedPrint
      _ <- delayedPrint
    yield ()

  private val programm2 = // parallel
    for
      fib1 <- delayedPrint.start
      fib2 <- delayedPrint.start
      _    <- fib1.join
      _    <- fib2.join
    yield ()

  private val programm3 = // cancelling fiber
    for
      fib <- delayedPrint.onCancel(IO(println("I am cancelled"))).start
      _   <- IO.sleep(500.millis) *> IO(println("cancelling fiber")) *> fib.cancel
      _   <- fib.join
    yield ()

  private val programm4 = // uncancellable fiber
    for
      fib <- IO.uncancelable(_ => delayedPrint.onCancel(IO(println("I am cancelled")))).start
      _   <- IO.sleep(500.millis) *> IO(println("cancelling fiber")) *> fib.cancel
      _   <- fib.join
    yield ()

  override def run =
    for
      _ <- programm1
      _ <- programm2
      _ <- programm3
      _ <- programm4
    yield ()
}
