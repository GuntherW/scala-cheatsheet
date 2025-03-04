package de.wittig.ox

import scala.concurrent.duration.*

  import ox.*

object StructuredConcurrency2 extends App {

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
}
