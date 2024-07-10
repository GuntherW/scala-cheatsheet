package de.wittig.ox

import ox.*
import ox.either.ok
import ox.channels.*
import ox.resilience.*
import scala.concurrent.duration.*
import scala.util.Random

object Main extends App {

  // run two computations in parallel
  def computation1: Int =
    sleep(2.seconds)
    println("computation1")
    1

  def computation2: String =
    sleep(1.second)
    println("computation2")
    "2"

  val resultPar: (Int, String) = par(computation1, computation2)
  println(resultPar)

  // timeout a computation
  def computation: Int =
    sleep(2.seconds)
    1

  // retry a computation
  def computationR: Int = Random.nextBoolean match
    case true  => throw new RuntimeException("boom!")
    case false => Random.nextInt

  val resultRetry = retry(RetryPolicy.backoff(3, 100.millis, 5.minutes, Jitter.Equal))(computationR)
  println(resultRetry)

  // create channels & transform them using high-level operations
  supervised {
    Source.iterate(0)(_ + 1) // natural numbers
      .transform(_.filter(_ % 2 == 0).map(_ + 1).take(10))
      .foreach(n => println(n.toString))
  }

  // select from a number of channels
  val c = Channel.rendezvous[Int]
  val d = Channel.rendezvous[Int]
  select(c.sendClause(10), d.receiveClause)

  // unwrap eithers and combine errors in a union type
  val v1: Either[Int, String]  = ???
  val v2: Either[Long, String] = ???

  val result: Either[Int | Long, String] = either:
    v1.ok() ++ v2.ok()

  // structured concurrency & supervision
  supervised {
    forkUser {
      println("fork1 start")
      sleep(1.second)
      println("fork1 end")
    }
    forkUser {
      println("fork2 start")
      sleep(500.millis)
      throw new RuntimeException("boom!")
    }
  }
  // on exception, ends the scope & re-throws

}
