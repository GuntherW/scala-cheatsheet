package de.codecentric.wittig.scala.cats.effects

import cats.effect.{ContextShift, IO, Timer}
import cats.implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

object Main extends App {

//  first()
//  fibStackSafe()
//  fromFuture()

//  readPrint()
  //flatMapOperator()

  parMapN

  def first() = {
    val ioa = IO { println("hey!") }
    val program: IO[Unit] =
      for {
        _ <- ioa
        _ <- ioa
      } yield ()
    program.unsafeRunSync()
  }

  def fibStackSafe() = {
    def fib(n: Int, a: Long = 0, b: Long = 1): IO[Long] =
      IO(a + b).flatMap { b2 =>
        if (n > 0)
          fib(n - 1, b, b2)
        else
          IO.pure(b2)
      }
    val program = fib(6)
    program.unsafeRunAsync(a => println(s"fib $a"))
  }

  def fromFuture() = {
    def inc(i: Int) = i + 1

    val f1: IO[Int] = IO.fromFuture(IO(Future(1)))
    val f2: IO[Int] = IO.fromFuture(IO(Future.failed(new Exception("lkk"))))

    f1.map(inc).unsafeRunAsync {
      case Left(t)  => println(s"Fehler $t:")
      case Right(i) => println(s"Erg: $i")
    }
    f2.map(inc).unsafeRunAsync {
      case Left(t)  => println(s"Fehler $t:")
      case Right(i) => println(s"Erg: $i")
    }
  }

  def readPrint() = {
    def putStrlLn(value: String) = IO.fromFuture(IO(Future(println(value))))
//    def putStrlLn(value: String) = IO(println(value))
    val readLn = IO(scala.io.StdIn.readLine)
    val programm = for {
      _ <- putStrlLn("What's your name?")
      n <- readLn
      _ <- putStrlLn(s"Hello, $n!")
    } yield ()

    programm.unsafeRunSync()
  }

  def flatMapOperator() = {

    // Needed for `sleep`
    implicit val timer = IO.timer(ExecutionContext.global)

    // Delayed println
    val io1: IO[Unit] = IO.sleep(5 seconds) *> IO(println("Hello!"))
    val io2: IO[Unit] =
      for {
        _ <- IO.sleep(5 seconds)
        e <- IO(println("Hello!"))
      } yield e

    io1.unsafeRunSync()
    io2.unsafeRunSync()
  }

  def shift() = {
    import scala.concurrent.ExecutionContext.Implicits.global

    implicit val contextShift = IO.contextShift(global)
    val task                  = IO(println("task"))

    IO.shift(contextShift).flatMap(_ => task)

    IO.shift *> task
  }

  def parMapN = {

    implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    implicit val timer: Timer[IO]     = IO.timer(ExecutionContext.global)

    case class Person(name: String, age: Int)
    val ioInt    = IO.sleep(2 seconds) *> IO(22)
    val ioString = IO.sleep(2 seconds) *> IO("Peter")

    val eins: IO[String] = ioInt *> ioString // IO("Peter") // flatMap
    val zwei: IO[Int]    = ioInt <* ioString // IO(22) // flatMap
    val drei: IO[String] = ioInt &> ioString // IO("Peter") // parallel
    val vier: IO[Int]    = ioInt <& ioString // IO(22) // parallel

    val sequenziell: IO[Person] = (ioString, ioInt).mapN(Person)
    val parallel: IO[Person]    = (ioString, ioInt).parMapN { case (a, b) => Person(a, b) } // needs implicit ContextShift

    val seq = for {
      start <- IO(System.nanoTime())
      a     <- sequenziell
      end   <- IO(System.nanoTime())
    } yield (a, end - start)

    val par = for {
      start <- IO(System.nanoTime())
      a     <- parallel
      end   <- IO(System.nanoTime())
    } yield (a, end - start)

    println(seq.unsafeRunSync()) // runs 4 seconds
    println(par.unsafeRunSync()) // runs 2 seconds

  }
}
