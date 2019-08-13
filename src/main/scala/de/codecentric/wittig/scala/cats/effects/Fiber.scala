package de.codecentric.wittig.scala.cats.effects

import cats.FlatMap.ops.toAllFlatMapOps
import cats.effect.{Fiber, IO, Timer}
import cats.syntax.flatMap.catsSyntaxFlatMapOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  *  A Fiber represents the (pure) result of an Async data type (e.g. IO) being started concurrently and that can be either joined or canceled.
  *  * You can think of fibers as being lightweight threads, a fiber being a concurrency primitive for doing cooperative multi-tasking.
  */
object Fiber extends App {
  // Needed for `start`
  implicit val ctx              = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global) // needed for IO.sleep

  // Just a small hint, how to create a Fiber.
  val io                         = IO(println("Hello!"))
  val fiber: IO[Fiber[IO, Unit]] = io.start
  // end Hint

  val launchMissiles = IO(println("missiles launched")) >> IO.sleep(5.second) *> IO.raiseError(new Exception("boom!"))
  val runToBunker    = IO(println("To the bunker!!!"))

  val end = for {
    fiber <- launchMissiles.start
    _ <- runToBunker.handleErrorWith { error =>
          // Retreat failed, cancel launch (maybe we should
          // have retreated to our bunker before the launch?)
          fiber.cancel *> IO.raiseError(error)
        }
    aftermath <- fiber.join
  } yield aftermath

  val end2 = for {
    _ <- launchMissiles.handleErrorWith { error =>
          println("i1")
          IO.raiseError(error)
//          IO.pure(1)
        }
    _ <- runToBunker.handleErrorWith { error =>
          println("inside") // wont be executed
          IO.raiseError(new Exception("Boom Inside"))
        }
  } yield ()

//  end.unsafeRunSync()
  end2.unsafeRunSync()
}
