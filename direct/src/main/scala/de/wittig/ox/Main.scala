package de.wittig.ox

import ox.*
import ox.either.ok
import ox.resilience.*
import ox.scheduling.Schedule

import scala.concurrent.duration.*
import scala.util.Random

object Main extends App {

  // run two computations in parallel
  private def computation1: Int =
    sleep(2.seconds)
    println("computation1")
    1

  private def computation2: String =
    sleep(1.second)
    println("computation2")
    "2"

  private val resultPar: (Int, String) = par(computation1, computation2)
  println(resultPar)

  // retry a computation
  private def computationR: Int =
    if (Random.nextBoolean) {
      throw new RuntimeException("boom!")
    } else {
      Random.nextInt
    }

  private val resultRetry = retry(Schedule.exponentialBackoff(100.millis))(computationR)
  println(resultRetry)

  private val v1: Either[Int, String]    = Left(1)
  private val v2: Either[String, String] = Left("eins")

  val result: Either[Int | String, String] = either:
    v1.ok() ++ v2.ok()
  println(result)

}
