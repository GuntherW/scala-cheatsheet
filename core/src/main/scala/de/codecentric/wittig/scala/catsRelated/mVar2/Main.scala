package de.codecentric.wittig.scala.catsRelated.mVar2

import cats.effect.{ContextShift, IO}

import scala.concurrent.ExecutionContext.Implicits.global
import cats.effect.concurrent._

/** Usage as Synchronized Mutable Variable */
object Main extends App {

  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  def sum(state: MVar2[IO, Int], list: List[Int]): IO[Int] =
    list match {
      case Nil     => state.take
      case x :: xs =>
        state.take.flatMap { current =>
          state.put(current + x).flatMap(_ => sum(state, xs))
        }
    }

  val list         = (0 until 100).toList
  val mVar         = MVar.of[IO, Int](0)
  val sum: IO[Int] = mVar.flatMap(sum(_, list))

  println(sum.unsafeRunSync())
}
