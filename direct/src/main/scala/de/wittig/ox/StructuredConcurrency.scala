package de.wittig.ox
import ox.*
import scala.concurrent.duration.*

object StructuredConcurrency extends App {

  val (f1, f2) = supervised {
    val f1 = fork {
      sleep(3.seconds)
      "f1"
    }

    val f2 = fork {
      sleep(2.second)
      "f2"
    }

    (f1.join(), f2.join())
  }
  println(s"Results - f1: $f1, f2: $f2")

  supervised {
    forkUser {
      println("fork1 start")
      sleep(1.second)
      println("fork1 end")
    }
    forkUser {
      println("fork2 start")
      sleep(500.millis)
      throw new RuntimeException("boom!")
    }
  }
  println("Ende b")
}
