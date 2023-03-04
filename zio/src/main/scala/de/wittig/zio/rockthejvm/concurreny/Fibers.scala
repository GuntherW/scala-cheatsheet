package de.wittig.zio.rockthejvm.concurreny

import zio.*
import de.wittig.zio.rockthejvm.util.*

import java.io.{File, FileWriter}

object Fibers extends ZIOAppDefault:

  val meaningOfLife = ZIO.succeed(42)
  val favLang       = ZIO.succeed("Scala")

  def sameThreadIO =
    for
      mol  <- meaningOfLife.debugThread
      lang <- favLang.debugThread
    yield (mol, lang)

  def differentThreadIO =
    for
      _ <- meaningOfLife.debugThread.fork
      _ <- favLang.debugThread
    yield ()

  val meaningOfLiveFiber: URIO[Any, Fiber[Throwable, Int]] = meaningOfLife.fork

  // join a fiber
  def runOnAnotherThread[R, E, A](zio: ZIO[R, E, A]) =
    for
      fib    <- zio.fork
      result <- fib.join
    yield result

  // await a fiber
  def runOnAnotherThreadV2[R, E, A](zio: ZIO[R, E, A]) =
    for
      fib    <- zio.fork
      result <- fib.await
    yield result

  // poll - peek at the result of the fiber right now, without blocking
  val peekFiber =
    for
      fib    <- ZIO.attempt {
                  Thread.sleep(1000)
                  42
                }.fork
      result <- fib.poll
    yield result

  // compose Fibers
  val zippedFibers =
    for
      fib1  <- ZIO.succeed("Result from fiber 1").debugThread.fork
      fib2  <- ZIO.succeed("Result from fiber 2").debugThread.fork
      fiber  = fib1.zip(fib2)
      tuple <- fiber.join
    yield tuple

  // orElse
  val chainedFibers =
    for
      fiber1  <- ZIO.fail("not good").debugThread.fork
      fiber2  <- ZIO.succeed("very good").debugThread.fork
      fiber    = fiber1.orElse(fiber2)
      message <- fiber.join
    yield message

  /*
   Exercises
   */

  // 1. zipo two fibers without using the zip combinator
  def zipFibers[E, A, B](fiber1: Fiber[E, A], fiber2: Fiber[E, B]): ZIO[Any, Nothing, Fiber[E, (A, B)]] =
    val finalEffect =
      for
        v1 <- fiber1.join
        v2 <- fiber2.join
      yield (v1, v2)
    finalEffect.fork

  // 2. write orElse combinator
  def chainFibers[E, A](fiber1: Fiber[E, A], fiber2: Fiber[E, A]): ZIO[Any, Nothing, Fiber[E, A]] =
    val waitFiber1 = fiber1.join
    val waitFiber2 = fiber2.join
    waitFiber1.foldZIO(
      e => waitFiber2.fork,
      a => ZIO.succeed(a).fork
    )

  // 3. distributing a task in between many fibers
  // spawn n fibers, count the n of words n each file
  // then aggregate all the results together in one big number
  def generateRandomFile(path: String): Unit =
    val random  = scala.util.Random
    val chars   = 'a' to 'z'
    val nWords  = random.nextInt(2000)
    val content = (1 to nWords)
      .map(_ => (1 to random.nextInt(10)).map(_ => chars(random.nextInt(26))).mkString)
      .mkString(" ")

    val writer = new FileWriter(new File(path))
    writer.write(content)
    writer.flush()
    writer.close()

  // part 1 - an effect wich reads one file and counts the words there
  def countWords(path: String): UIO[Int] =
    ZIO.succeed {
      val source = scala.io.Source.fromFile(path)
      val nWords = source.getLines().mkString(" ").split(" ").count(_.nonEmpty)
      println(s"[${Thread.currentThread().getName}] Counted $nWords words in $path")
      source.close()
      nWords
    }

  // part 2 - spin up fibers for all the files
  def wordCountParallel(n: Int): UIO[Int] =
    val effects = (1 to n)
      .map(i => s"zio/src/main/resources/testfile_$i.txt")
      .map(countWords)
      .map(_.fork)
      .map(fiberEff => fiberEff.flatMap(_.join))
    effects.reduce { (zioa, ziob) =>
      for
        a <- zioa
        b <- ziob
      yield a + b
    }

  def run =
    for
//      _ <- sameThreadIO.debugThread
//      _ <- differentThreadIO.debugThread
//      _ <- runOnAnotherThread(meaningOfLife).debugThread
//      _ <- runOnAnotherThreadV2(meaningOfLife).debugThread
//      _ <- peekFiber.debugThread
//      _ <- zippedFibers.debugThread
      _ <- chainedFibers.debugThread
    yield ()

// generate Files for exercise
//  def run = ZIO.succeed((1 to 10).foreach(i => generateRandomFile(s"zio/src/main/resources/testfile_$i.txt")))
//  def run = wordCountParallel(10).debugThread
