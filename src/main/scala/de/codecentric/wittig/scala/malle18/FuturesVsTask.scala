package de.codecentric.wittig.scala.malle18

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object FuturesVsTask extends App {

  val map1: Int => Int = _ + 1
  val map2: Int => String = _.toString
  val map3: String => Int = _.toInt

  val amount = 1000

  time("future") {
    val a = Future.sequence((1 to amount).map(future))
    Await.result(a, Duration.Inf)
  }

  time("task") {
    val a = Task.sequence((1 to amount).map(task))
    Await.result(a.runToFuture, Duration.Inf)
  }

  private def future(i: Int) =
    Future(i)
      .map(map1)
      .map(map2)
      .map(map3)
      .map(map1)

  private def task(i: Int) =
    Task(i)
      .map(map1)
      .map(map2)
      .map(map3)
      .map(map1)
}
