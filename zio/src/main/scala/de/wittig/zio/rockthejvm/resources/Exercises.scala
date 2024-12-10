package de.wittig.zio.rockthejvm.resources

import zio.*
import Resources.*
import de.wittig.zio.rockthejvm.util.debugThread

import java.io.File
import java.util.Scanner

object Exercises extends ZIOAppDefault:
  val path = "zio/src/main/scala/de/wittig/zio/rockthejvm/resources/Resources.scala"

  /** Exercises
    *   1. Use the acquireRelease to open a file, print all lines, (one every 100 millis), then close the file
    */
  def openFileScanner(path: String): UIO[Scanner] =
    ZIO.succeed(new Scanner(new File(path)))

  def readLineByLine(scanner: Scanner): UIO[Unit] =
    if (scanner.hasNextLine)
      ZIO.succeed(scanner.nextLine()).debugThread *> ZIO.sleep(100.millis) *> readLineByLine(scanner)
    else
      ZIO.unit

  def acquireOpenFile(path: String): UIO[Unit] =
    ZIO.succeed(s"opening file at $path").debugThread *>
      ZIO.acquireReleaseWith(
        openFileScanner(path) // acquire
      )(scanner =>
        ZIO.succeed(s"closing file at $path").debugThread *> ZIO.succeed(scanner.close()) // close
      )(readLineByLine)

  val testInterruptFileDisplay = for {
    fib <- acquireOpenFile(path).fork
    _   <- ZIO.sleep(2.seconds) *> fib.interrupt
  } yield ()

  // acquireRelease vs acquireReleaseWith
  def connFromConfig(path: String): UIO[Unit] =
    ZIO.acquireReleaseWith(openFileScanner(path))(scanner => ZIO.succeed("closing file").debugThread *> ZIO.succeed(scanner.close())) { scanner =>
      ZIO.acquireReleaseWith(Connection.create(scanner.nextLine()))(_.close()) { conn =>
        conn.open() *> ZIO.never
      }
    }

  // nested resource
  def connFromConfig_v2(path: String): UIO[Unit] = ZIO.scoped {
    for {
      scanner <- ZIO.acquireRelease(openFileScanner(path))(scanner => ZIO.succeed("closing file").debugThread *> ZIO.succeed(scanner.close()))
      conn    <- ZIO.acquireRelease(Connection.create(scanner.nextLine()))(_.close())
      _       <- conn.open() *> ZIO.never
    } yield ()
  }

//  def run = testInterruptFileDisplay
  def run = connFromConfig_v2(path)
