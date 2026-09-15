package de.wittig.ox

import scala.concurrent.duration.*

import ox.*

@main
def structuredConcurrency2(): Unit =

  // supervised Block ist fertig, wenn forKUser fertig ist. fork läuft so lange wie der supervised block aktiv ist und beendet sich dann auch automatisch.
  val a = supervised {
    fork {
      while (true) {
        println("catching Metrics ...")
        sleep(500.millis)
      }
    }
    forkUser {
      println("fork1 start")
      sleep(2.second)
      println("fork1 end")
    }
  }
