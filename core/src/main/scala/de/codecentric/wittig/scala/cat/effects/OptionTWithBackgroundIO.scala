package de.codecentric.wittig.scala.cat.effects
import cats.data.OptionT
import cats.effect.{IO, IOApp}
import scala.concurrent.duration.DurationInt
import cats.syntax.all.toFunctorOps

object OptionTWithBackgroundIO extends IOApp.Simple {

  def run: IO[Unit] = run2

  def run1: IO[Unit] = {
    val optionT: OptionT[IO, String] = OptionT.some[IO]("Result from OptionT")
    val backgroundIO: IO[Unit]       = IO.println("Running in the background...")

    // Combine OptionT and the background IO
    val result: IO[Option[String]] = for {
      _     <- backgroundIO.start // Start the background IO
      value <- optionT.value      // Extract the OptionT's value
    } yield value

    // Print the result of the OptionT
    result.flatMap {
      case Some(value) => IO.println(s"OptionT value: $value")
      case None        => IO.println("OptionT returned None.")
    }
  }

  def run2: IO[Unit] = {
    val optionT: OptionT[IO, String] = OptionT.some[IO]("Result from OptionT")
    val backgroundIO: IO[Unit]       = IO.println("Running in the background...")

    // Combine OptionT and the background IO
    val result = for {
      ot    <- optionT                     // Start the background IO
      value <- OptionT.liftF(backgroundIO) // Extract the OptionT's value
    } yield ot

    val b = result.value.debug
    IO.pure(())
  }
}
