package de.codecentric.wittig.scala.malle18

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object TaskSeqVsPar extends App {

  val timeInSeconds = 1000

  val t1: Task[Int] = createTask(1)
  val t2: Task[Int] = createTask(2)
  val t3: Task[Int] = createTask(3)

  // Erster Versuch
  time("3 Tasks sequenziell") {
    val seq = for {
      i1 <- t1
      i2 <- t2
      i3 <- t3
    } yield i1 + i2 + i3
    Await.result(seq.runToFuture, Duration.Inf)
  }

  // Zweiter Versuch
  time("3 Tasks parallel") {
    val par = Task.gather(List(t1, t2, t3)).map(l => l.sum)
    Await.result(par.runToFuture, Duration.Inf)
  }

  private def createTask(i: Int) = Task {
    Thread.sleep(timeInSeconds)
    i
  }
}
