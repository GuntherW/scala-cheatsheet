package de.codecentric.wittig.scala.akka.stream

import akka.actor.ActorSystem
import akka.stream.ThrottleMode
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}

/** Example from Heiko Seeberger
  */
object GreetGood extends App {
  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher

  val done =
    Source
      .repeat("Learn you Akka Streams for great Good!")
      .zip(Source.fromIterator(() => Iterator.from(0)))
      .take(7)
      .mapConcat { case (s, n) =>
        val i = " " * n
        f"$i$s%n"
      }
      .throttle(42, 500.millis, 1, ThrottleMode.Shaping)
      .toMat(Sink.foreach(print))(Keep.right) // Keep Right für den materialized Value vom Sink.
      .run()

  done.onComplete(_ => system.terminate())
  Await.ready(system.whenTerminated, Duration.Inf)
}
