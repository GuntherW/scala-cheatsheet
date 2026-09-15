package de.wittig.ox

import ox.*
import ox.either.{catching, ok}
import ox.resilience.*
import ox.scheduling.{repeat, Schedule}

import scala.concurrent.TimeoutException
import scala.concurrent.duration.*
import scala.util.Random

@main
def main(): Unit =

  parallel()
  retrying()
  eitherOx()
  racing()
  timeOutOx()
  repeating()

  // run two computations in parallel
  def parallel(): Unit =
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

  def retrying(): Unit =
    def computationR: Int = if Random.nextBoolean() then throw new RuntimeException("boom!") else Random.nextInt()

    val resultRetry = retry(Schedule.exponentialBackoff(100.millis))(computationR)
    println(s"retrying: $resultRetry")

  def eitherOx(): Unit =
    val v1: Either[Int, String]    = Left(1)
    val v2: Either[String, String] = Left("eins")

    val result: Either[Int | String, String] = either:
      v1.ok() ++ v2.ok()
    println(result)

  def racing(): Unit =
    def computation1: Int = { sleep(1.seconds); 1 }
    def computation2: Int = { sleep(2.second); 2 }

    val res: Int = raceSuccess(computation1, computation2)
    println(s"racing: $res")

  def timeOutOx(): Unit =
    def computation3: Int = { sleep(2.seconds); 1 }

    val res: Either[TimeoutException, Int] = timeout(1.second)(computation3).catching[TimeoutException]
    println(s"timeout: $res")

  def repeating(): Unit =
    def computationR: Int =
      val i = Random.nextInt()
      println(s"repeating ($i)")
      i

    repeat(Schedule.fixedInterval(100.millis).maxAttempts(5))(computationR)
