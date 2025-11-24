package de.wittig.ox

import scala.concurrent.duration.*

import ox.*

@main
def structuredConcurrency2(): Unit =

  val a = supervised {
    forkUser {
      println("fork1 start")
      sleep(2.second)
      println("fork1 end")
    }
    fork {
      println("fork2 start")
      sleep(2.seconds)
      println("fork2 end")
    }
  }
