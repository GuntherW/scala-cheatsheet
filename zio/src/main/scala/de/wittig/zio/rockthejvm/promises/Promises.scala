package de.wittig.zio.rockthejvm.promises

import de.wittig.zio.rockthejvm.util.debugThread
import zio.*

object Promises extends ZIOAppDefault {

  val aPromise = Promise.make[Throwable, Int]

  // await - block the fiber until the promise has a value
  val reader = aPromise.flatMap { promise =>
    promise.await
  }

  // producer - consumer problem
  def demoPromise() =
    def consumer(promise: Promise[Throwable, Int]) =
      for
        _   <- ZIO.succeed("consumer waiting for reuslt").debugThread
        mol <- promise.await
        _   <- ZIO.succeed(s"consumer I gut the result $mol").debugThread
      yield ()

    def producer(promise: Promise[Throwable, Int]) =
      for
        _   <- ZIO.succeed("producer crunching numbers").debugThread
        _   <- ZIO.sleep(3.seconds)
        _   <- ZIO.succeed("produder complete").debugThread
        mol <- ZIO.succeed(42)
        _   <- promise.succeed(mol)
      yield ()

    for
      promise <- Promise.make[Throwable, Int]
      _       <- consumer(promise).zipPar(producer(promise))
    yield ()

  def run = demoPromise()
}
