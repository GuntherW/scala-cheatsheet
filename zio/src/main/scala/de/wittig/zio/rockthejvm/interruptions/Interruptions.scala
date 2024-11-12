package de.wittig.zio.rockthejvm.interruptions

import de.wittig.zio.rockthejvm.util.debugThread
import zio.*

object Interruptions extends ZIOAppDefault:

  val zioWithTime =
    (
      ZIO.succeed("starting computation").debugThread *>
        ZIO.sleep(2.seconds) *>
        ZIO.succeed(42).debugThread
    )
      .onInterrupt(ZIO.succeed("I was interrupted").debugThread)

  val interruption =
    for
      fib    <- zioWithTime.fork
      _      <- ZIO.sleep(1.second) *> ZIO.succeed("Interrupting").debugThread *> fib.interrupt // is an effect, blocking the calling fiber
      _      <- ZIO.succeed("Interruption successful").debugThread
      result <- fib.join
    yield result

  val interruptionV2 =
    for
      fib    <- zioWithTime.fork
      _      <- ZIO.sleep(1.second) *> ZIO.succeed("Interrupting").debugThread *> fib.interruptFork
      _      <- ZIO.succeed("Interruption successful").debugThread
      result <- fib.join
    yield result

  // Automatic interruption
  // outliving parents
  val parentEffect =
    ZIO.succeed("spawning a fiber").debugThread *>
      zioWithTime.fork *>       // will be interrupted
      zioWithTime.forkDaemon *> // will be a child of the MAIN fiber and not be interrupted
      ZIO.sleep(1.second) *>    // only 1 second. Will interrupt zioWithTime automatically
      ZIO.succeed("parent successful").debugThread

  val testOutlivingParents =
    for
      parentEffectFib <- parentEffect.fork
      _               <- ZIO.sleep(3.seconds)
      _               <- parentEffectFib.join
    yield ()

  // racing
  val slowEffect = (ZIO.sleep(2.seconds) *> ZIO.succeed("slow").debugThread).onInterrupt(ZIO.succeed("[slow] interrupted").debugThread)
  val fastEffect = (ZIO.sleep(1.seconds) *> ZIO.succeed("fast").debugThread).onInterrupt(ZIO.succeed("[fast] interrupted").debugThread)
  val aRace      = slowEffect.race(fastEffect)
  val testRace   = aRace.fork *> ZIO.sleep(3.seconds)

  def run =
    testOutlivingParents *>
      testRace
