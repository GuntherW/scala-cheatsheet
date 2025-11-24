package de.wittig.ox

import ox.*
import ox.either.ok
import ox.resilience.*
import ox.scheduling.Schedule

import scala.concurrent.duration.*
import scala.util.Random

@main
def main(): Unit =

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
  def computationR: Int =
    if (Random.nextBoolean) {
      throw new RuntimeException("boom!")
    } else {
      Random.nextInt
    }

  val resultRetry = retry(Schedule.exponentialBackoff(100.millis))(computationR)
  println(resultRetry)

  val v1: Either[Int, String]    = Left(1)
  val v2: Either[String, String] = Left("eins")

  val result: Either[Int | String, String] = either:
    v1.ok() ++ v2.ok()
  println(result)
