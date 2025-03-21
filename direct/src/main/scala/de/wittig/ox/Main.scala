package de.wittig.ox

import scala.concurrent.duration.*
import scala.util.Random

import ox.*
import ox.channels.*
import ox.either.ok
import ox.resilience.*
import ox.scheduling.Jitter

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

  // retry a computation
  def computationR: Int = Random.nextBoolean match
    case true  => throw new RuntimeException("boom!")
    case false => Random.nextInt

  val resultRetry = retry(RetryConfig.backoff(3, 100.millis, 5.minutes, Jitter.Equal))(computationR)
  println(resultRetry)

  val v1: Either[Int, String]    = Left(1)
  val v2: Either[String, String] = Left("eins")

  val result: Either[Int | String, String] = either:
    v1.ok() ++ v2.ok()
  println(result)

}
