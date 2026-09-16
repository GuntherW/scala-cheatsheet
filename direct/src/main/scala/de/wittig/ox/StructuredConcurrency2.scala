package de.wittig.ox

import scala.concurrent.duration.*

import ox.*
import ox.scheduling.{repeat, Schedule}

@main
def structuredConcurrency2(): Unit =

  // supervised Block ist fertig, wenn forKUser fertig ist. fork läuft so lange wie der supervised block aktiv ist und beendet sich dann auch automatisch.
  val a = supervised {
    fork {
      repeat(Schedule.fixedInterval(500.millis)) {
        println("catching Metrics ...")
      }
    }
    forkUser {
      println("fork1 start")
      sleep(2.second)
      println("fork1 end")
    }
  }
