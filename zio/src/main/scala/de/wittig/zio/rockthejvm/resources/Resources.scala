package de.wittig.zio.rockthejvm.resources

import de.wittig.zio.rockthejvm.util.debugThread
import zio.*

object Resources extends ZIOAppDefault {

  def unsafeMethod(): Int = throw new RuntimeException("Not an int here")
  val anAttempt           = ZIO.attempt(unsafeMethod())

  // finalizers
  val attemptWithFinaliser = anAttempt.ensuring(ZIO.succeed("finalizer").debugThread)

  // multiple finalizers
  val attemptWith2Finalizers = attemptWithFinaliser.ensuring(ZIO.succeed("another finalizer").debugThread)
  // .onInterrupt, .onError, .onDone, .onExit

  // resource lifecycle
  class Connection(url: String):
    def open()  = ZIO.succeed(s"opening connection to $url").debugThread
    def close() = ZIO.succeed(s"closing connection to $url").debugThread

  object Connection:
    def create(url: String) = ZIO.succeed(Connection(url))

  val fetchUrl =
    for
      conn <- Connection.create("rockthejvm.com")
      fib  <- (conn.open() *> ZIO.sleep(300.seconds)).fork
      _    <- ZIO.sleep(1.second) *> ZIO.succeed("interrupting").debugThread *> fib.interrupt
      _    <- fib.join
    yield () // resource leak

  // correct, but tedious
  val correctFetchUrl =
    for
      conn <- Connection.create("rockthejvm.com")
      fib  <- (conn.open() *> ZIO.sleep(300.seconds)).ensuring(conn.close()).fork
      _    <- ZIO.sleep(1.second) *> ZIO.succeed("interrupting").debugThread *> fib.interrupt
      _    <- fib.join
    yield ()

  // clean way
  val cleanConnection   = ZIO.acquireRelease(Connection.create("rockthejvm.com"))(_.close())
  val fetchWithResource =
    for
      conn <- cleanConnection
      fib  <- (conn.open() *> ZIO.sleep(300.seconds)).fork
      _    <- ZIO.sleep(1.second) *> ZIO.succeed("interrupting").debugThread *> fib.interrupt
      _    <- fib.join
    yield ()

  // eleminate ZIO default scoping
  val fetchWithScoopoedResource = ZIO.scoped(fetchWithResource)

  val cleanConnectionV2 =
    ZIO.acquireReleaseWith(
      Connection.create("rockthejvm.com")
    )(
      _.close()
    )(conn => conn.open() *> ZIO.sleep(300.seconds))

  val fetchWithResourceV2 =
    for
      fib <- cleanConnectionV2.fork
      _   <- ZIO.sleep(1.second) *> ZIO.succeed("interrupting").debugThread *> fib.interrupt
      _   <- fib.join
    yield ()

  //  def run = attemptWith2Finalizers.debugThread
//  def run = fetchUrl
//  def run = correctFetchUrl
//  def run = fetchWithResource
  def run = fetchWithResourceV2
}
