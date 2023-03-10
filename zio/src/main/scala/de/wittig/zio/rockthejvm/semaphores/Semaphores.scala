package de.wittig.zio.rockthejvm.semaphores

import de.wittig.zio.rockthejvm.util.debugThread
import zio.*

object Semaphores extends ZIOAppDefault {

  // n permits
  // acquire, acquireN
  // release, releaseN

  // limiting the number of concurrent sessions on a server
  def doWorkWhileLoggedIn() =
    ZIO.sleep(1.second) *> Random.nextIntBounded(100)

  def login(id: Int, sem: Semaphore) =
    ZIO.succeed(s"[task $id] waiting to log in").debugThread *>
      sem.withPermit {
        for
          _   <- ZIO.succeed(s"[task $id] logged in, working ...").debugThread
          res <- doWorkWhileLoggedIn()
          _   <- ZIO.succeed(s"[task $id] done").debugThread
        yield res
      }

  def demoSemaphore() =
    for
      sem <- Semaphore.make(2) // Semaphore.make(1) == a Mutex
      f1  <- login(1, sem).fork
      f2  <- login(2, sem).fork
      f3  <- login(3, sem).fork
      _   <- f1.join
      _   <- f2.join
      _   <- f3.join
    yield ()

  def run = demoSemaphore()
}
