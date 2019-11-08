package de.codecentric.wittig.scala.futur

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success
import de.codecentric.wittig.scala.Implicits._

/**
  * @author gunther
  */
object TestFuture extends App {
  val randomNumber = 42

  println("test1: " + test1.await == randomNumber)
  println("test2: " + test2.await)

  def test1: Future[Int] = {
    val f = Future(randomNumber)
    f.andThen {
        case _ =>
          println("andthen 1")
          throw new IllegalArgumentException("dd")
      }
      .andThen {
        case Failure(t) => println("andthen 2 failure" + t) // not called.
        case Success(v) => println("andthen 2 success" + v)
      }
    f
  }

  def test2 = {
    val f1 = Future(randomNumber)
    val f2 = Future(new Exception("Boom"))

    val u1: Unit = f1.failed.foreach(t => println(t))
    val u2: Unit = f2.foreach(i => println(i))

    Future.sequence(List(f1, f2))
  }
}
