package de.wittig.zio.rockthejvm.concurreny

import de.wittig.zio.rockthejvm.util.debugThread
import zio.*

import java.util.concurrent.atomic.AtomicBoolean

object BlockingEffects extends ZIOAppDefault:

  def blockintTask(n: Int): UIO[Unit] =
    ZIO.succeed(s"running blocking task $n").debugThread *>
      ZIO.succeed(Thread.sleep(10000)) *> // Thread.sleep ist blockend (ZIO.sleep wäre es nicht)
      blockintTask(n)

  val programm = ZIO.foreachPar((1 to 100).toList)(blockintTask)
  // thread starvation

  // blocking thread pool
  val aBlockingZIO = ZIO.attemptBlocking {
    println(s"[${Thread.currentThread().getName}] running a long computation")
    Thread.sleep(10000)
    42
  }

  // blocking code cannot (usually) be interrupted
  val tryInterrupting =
    for
      blockingFib <- aBlockingZIO.fork
      _           <- ZIO.sleep(1.second) *> ZIO.succeed("interrupting...").debugThread *> blockingFib.interrupt
      mol         <- blockingFib.join
    yield mol

  // interrupt blocking
  val aBlockingInterruptibleZIO = ZIO.attemptBlockingInterrupt {
    println(s"[${Thread.currentThread().getName}] running a long computation")
    Thread.sleep(10000)
    42
  }

  val tryInterrupting2 =
    for
      blockingFib <- aBlockingInterruptibleZIO.fork
      _           <- ZIO.sleep(1.second) *> ZIO.succeed("interrupting...").debugThread *> blockingFib.interrupt
      mol         <- blockingFib.join
    yield mol

  // set a flag/switch
  def interruptibleBlockingEffects(cancelledFlag: AtomicBoolean): Task[Unit] =
    ZIO.attemptBlockingCancelable { // effect
      (1 to 100000).foreach { element =>
        if (!cancelledFlag.get()) {
          println(element)
          Thread.sleep(100)
        }
      }
    }(ZIO.succeed(cancelledFlag.set(true))) // cancelling/interrupting effect

  val interruptibleBlocking =
    for
      fib <- interruptibleBlockingEffects(new AtomicBoolean(false)).fork
      _   <- ZIO.sleep(2.seconds) *> ZIO.succeed("interrupting...").debugThread *> fib.interrupt
      _   <- fib.join
    yield ()

  // Semantic blocking - no blocking of threads, descheduling the effect/fiber
  val sleeping       = ZIO.sleep(1.second)             // semantically blocking, interruptible
  val sleepingThread = ZIO.succeed(Thread.sleep(1000)) // blocking, uninterruptible
  // yielding
  val chainedZIO     = (1 to 10000).map(i => ZIO.succeed(i)).reduce(_.debugThread *> _.debugThread)
  val yieldingDemo   = (1 to 10000).map(i => ZIO.succeed(i)).reduce(_.debugThread *> ZIO.yieldNow *> _.debugThread)

//  def run = programm
//  def run = aBlockingZIO
//  def run = tryInterrupting
//  def run = tryInterrupting2
//  def run = interruptibleBlocking
//  def run = chainedZIO
  def run = yieldingDemo
