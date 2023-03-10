package de.wittig.zio.rockthejvm.interruptions

import de.wittig.zio.rockthejvm.util.debugThread
import zio.*

object MasteringInterruptions extends ZIOAppDefault {

  // manual interruption
  val aManuallyInterruptedZIO = ZIO.succeed("computing ...").debugThread *> ZIO.interrupt *> ZIO.succeed(42).debugThread

  // finalizer
  val effectWithINterruptionFinalizer = aManuallyInterruptedZIO.onInterrupt(ZIO.succeed("I was interrupted").debugThread)

  // uninterruptible
  val fussyPaymentSystem =
    (for
      _ <- ZIO.succeed("payment running, don't cancel mee...").debugThread
      _ <- ZIO.sleep(1.seconds)
      _ <- ZIO.succeed("payment succeeded").debugThread
    yield ())
      .onInterrupt(ZIO.succeed("MEGA CANCEL OF DOOM").debugThread)

  val cancellationOfDoom =
    for
      fib <- fussyPaymentSystem.fork
      _   <- ZIO.sleep(500.millis) *> fib.interrupt
      _   <- fib.join
    yield ()

  val atomicPayment        = ZIO.uninterruptible(fussyPaymentSystem)
  val atomicPaymentV2      = fussyPaymentSystem.uninterruptible // same
  val noCancellationOfDoom =
    for
      fib <- atomicPayment.fork
      _   <- ZIO.sleep(500.millis) *> fib.interrupt
      _   <- fib.join
    yield ()

//  def run = aManuallyInterruptedZIO
//  def run                = effectWithINterruptionFinalizer
//  def run                  = cancellationOfDoom
  def run = noCancellationOfDoom
}
