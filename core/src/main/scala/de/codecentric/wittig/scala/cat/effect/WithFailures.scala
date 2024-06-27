package de.codecentric.wittig.scala.cat.effect

import cats.effect.{IO, IOApp}

object WithFailures extends IOApp.Simple {

  private val a =
    for
      _    <- IO.println("start a")
      eins <- IO(1)
      _    <- IO.raiseError(new IllegalStateException("Boom"))
      _    <- IO.println("end a")
    yield eins

  private val b =
    for
      _    <- IO.println("start b")
      eins <- IO(1)
      _    <- IO.println("end b")
    yield eins

  val programm = a *> b *> IO(println("Hallo Welt"))

  override def run: IO[Unit] = programm.onError(_ => IO.println("Jupp, Fehler passieren"))
}
