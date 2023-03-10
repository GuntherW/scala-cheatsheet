package de.wittig.zio.rockthejvm.schedules

import de.wittig.zio.rockthejvm.util.debugThread
import zio.*

object Schedules extends ZIOAppDefault {

  val aZIO = Random.nextBoolean.flatMap { flag =>
    if (flag) ZIO.succeed("fetched value").debugThread
    else ZIO.succeed("Failure...").debugThread *> ZIO.fail("error")
  }

  val aRetriedZIO = aZIO.retry(Schedule.recurs(10)) // tries 10 times, returns the first success, last failure

  val oneTimeSchedule       = Schedule.once
  val recurrentSchedule     = Schedule.recurs(10)
  val fixedIntervalSchedule = Schedule.spaced(1.second) // retries every 1s until a success is returned

  val exBackOffSchedule = Schedule.exponential(1.second, 2.0)
  val fiboSchedule      = Schedule.fibonacci(1.second)

  // combinators
  val recurrentAndSpaced = Schedule.recurs(3) && Schedule.spaced(1.second)

  // sequencing
  val recurrentThenSpaced = Schedule.recurs(3) ++ Schedule.spaced(1.second) // 3 retries, then 1s

  val totalElapsed = Schedule.spaced(1.second) >>> Schedule.elapsed.map(time => println(s"total time elapsed: $time"))

//  def run = aRetriedZIO
//  def run = aZIO.retry(fixedIntervalSchedule)
  def run = aZIO.retry(totalElapsed)
}
