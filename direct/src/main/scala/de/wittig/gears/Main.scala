package de.wittig.gears

import scala.collection.mutable
import scala.concurrent.duration.*

import gears.async.*
import gears.async.default.given

object Main extends App:

//  simpleExample()
//  sleepExample()
  sleepSort()

  private inline def simpleExample(): Unit =
    Async.blocking:
      val hello = Future:
        print("Hello")
      val world = Future:
        hello.await
        println(", world!")
      world.await

  private inline def sleepExample(): Unit =
    /** Counts to [[n]], sleeping for 100milliseconds in between. */
    def countTo(n: Int)(using Async): Unit =
      (1 to n).foreach: i =>
        AsyncOperations.sleep(100) /*(using Async)*/
        println(s"counted $i")

    Async.blocking:
      countTo(10)
      println("Finished counting!")

  private inline def sleepSort(): Unit =
    Async.blocking:
      val origin = Seq(50, 80, 10, 60, 40, 100)
      // Spawn sleeping futures!
      val buf    = mutable.ArrayBuffer[Int]()
      origin.map: n =>
        Future:
          AsyncOperations.sleep(n.millis)
          buf.synchronized:
            buf += n
      .awaitAll
      println(buf)
