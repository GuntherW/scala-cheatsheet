package de.codecentric.wittig.scala.futur

import de.codecentric.wittig.scala.Implicits.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

/** @author
  *   gunther
  */
object TestFuture extends App:

  val randomNumber = 42

  def test1: Future[Int] =
    val f = Future(randomNumber)
    f.andThen {
      case _ =>
        println("andthen 1")
        throw new IllegalArgumentException("dd")
    }.andThen {
      case Failure(t) => println("andthen 2 failure" + t) // not called.
      case Success(v) => println("andthen 2 success" + v)
    }
    f

  private val f1 = Future(randomNumber)
  private val f2 = Future(new Exception("Boom"))

  val u1: Unit = f1.failed.foreach(t => println(t))
  val u2: Unit = f2.foreach(i => println(i))

  val test2 = Future.sequence(List(f1, f2))

//  println(test1.await == randomNumber)
  println(test2.await)
