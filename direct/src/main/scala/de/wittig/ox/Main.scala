package de.wittig.ox

import ox.*
import ox.either.{catching, ok}
import ox.resilience.*
import ox.scheduling.{repeat, Schedule}

import java.time.{LocalDateTime, LocalTime}
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
  rateLimiting()

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

  def rateLimiting(): Unit =
    supervised:
      // Erlaubt maximal 2 Operationen pro 1 Sekunde (Fixed Window mit Startzeit-Tracking)
      val rateLimiter = RateLimiter.fixedWindowWithStartTime(2, 1.second)

      def operation(name: String): String =
        println(s"RateLimiting: ${LocalTime.now()} Executing $name")
        s"Result of $name"

      // runBlocking: blockiert, falls das Rate-Limit erreicht ist, bis wieder ein Slot frei wird
      val res1 = rateLimiter.runBlocking(operation("op1"))
      val res2 = rateLimiter.runBlocking(operation("op2"))
      val res3 = rateLimiter.runBlocking(operation("op3")) // wartet bis zum nächsten Fenster
      println(s"RateLimiting: runBlocking results: $res1, $res2, $res3")

      sleep(1.second) // Um das Zeitfenster wieder "neu" zu setzen. (Denn op3 ist ja bereits im neuen Zeitfenster)

      // runOrDrop: bricht sofort mit None ab, falls das Rate-Limit überschritten ist, ansonsten Some(result)
      val drop1 = rateLimiter.runOrDrop(operation("op4"))
      val drop2 = rateLimiter.runOrDrop(operation("op5"))
      val drop3 = rateLimiter.runOrDrop(operation("op6")) // wird verworfen -> None
      println(s"RateLimiting: runOrDrop results: $drop1, $drop2, $drop3")
