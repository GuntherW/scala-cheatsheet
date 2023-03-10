package de.wittig.zio.rockthejvm.refs

import de.wittig.zio.rockthejvm.util.debugThread
import zio.*

import java.util.concurrent.TimeUnit

object Refs extends ZIOAppDefault {

  val atomicMOL = Ref.make(42)

  // get set
  val mol       = atomicMOL.flatMap(_.get)
  val setMol    = atomicMOL.flatMap(_.set(100))
  val getSetMol = atomicMOL.flatMap(_.getAndSet(101))

  // update
  val updateMol              = atomicMOL.flatMap(_.update(_ * 100))
  val updatedMolWithNewValue = atomicMOL.flatMap(_.updateAndGet(_ * 100))
  val updatedMolWithOldValue = atomicMOL.flatMap(_.getAndUpdate(_ * 100))

  // modify
  val modifiedMol = atomicMOL.flatMap(_.modify(i => (s"my current value is $i", i * 100)))

  def demoConcurrentWork() =
    def task(workload: String, totalCount: Ref[Int]) =
      val wordCount = workload.split(" ").length
      for
        _        <- ZIO.succeed(s"counting words for $workload: $wordCount").debugThread
        newCount <- totalCount.updateAndGet(_ + wordCount)
        _        <- ZIO.succeed(s"new total: $newCount").debugThread
      yield ()

    for
      counter <- Ref.make(0)
      _       <- ZIO.collectAllParDiscard(
                   List(
                     "Hallo Welt",
                     "Guten Morgen, liebe Sorgen",
                     "Morgenstund hat Gold im Mund"
                   ).map(load => task(load, counter))
                 )
    yield counter

  /** Exercise */
  def printTimeAndIncTick(tick: Ref[Int]) =
    for
      time <- Clock.currentTime(TimeUnit.MILLISECONDS)
      _    <- Console.printLine(s"current time: $time")
      _    <- tick.update(_ + 1)
    yield ()

  def printTicks(tick: Ref[Int]) =
    for
      tick <- tick.get
      _    <- Console.printLine(s"current tick: $tick")
    yield ()

  val programm =
    for
      ticker   <- Ref.make(0)
      printTime = printTimeAndIncTick(ticker).repeat(Schedule.spaced(1.second))
      printTick = printTicks(ticker).repeat(Schedule.spaced(5.seconds))
      _        <- printTime.zipPar(printTick)
    yield ()

  def run = programm
}
