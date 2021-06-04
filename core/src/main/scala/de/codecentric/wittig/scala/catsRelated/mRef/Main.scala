package de.codecentric.wittig.scala.catsRelated.mRef

import cats.effect.{ContextShift, IO}

import scala.concurrent.ExecutionContext.Implicits.global
import cats.effect.concurrent._

/** Usage as Synchronized Mutable Variable */
object Main extends App {

  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  val ref = Ref.of[IO, Int](0)

  ref.flatMap(_.set(100))
  val v = ref.flatMap(_.get)

  println(v.unsafeRunSync()) // still 0

  val app = for {
    ref <- Ref.of[IO, Int](0)
    _   <- ref.set(100)
    v   <- ref.get
    _   <- IO(println(v))
  } yield ()
  app.unsafeRunSync()
}
