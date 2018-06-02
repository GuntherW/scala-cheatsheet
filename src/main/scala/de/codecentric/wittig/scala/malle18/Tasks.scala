package de.codecentric.wittig.scala.malle18

import cats.implicits._
import monix.eval._
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Tasks extends App {

  def future1 = Future {
    Thread.sleep(5000)
    println("future1")
    1
  }

  def future2 = Future {
    Thread.sleep(5000)
    println(s"future2")
    2
  }

  val task1 = Task {
    Thread.sleep(5000)
    println("task1")
    1
  }
  def task2 = Task {
    Thread.sleep(5000)
    println("task2")
    2
  }

  val t: Task[Int] = (task1, task2, Task(3)).mapN((x, y, _) => x + y)

  def twice[A](t: Task[A]) = {
    for {
      _ <- t
      _ <- t
    } yield ()
  }

  def lazyTwice[A](t: Task[A]) = {
    val lazyTask = t.memoize
    for {
      _ <- lazyTask
      _ <- lazyTask
    } yield ()
  }

  twice(task1.memoize)

  val task3: Task[(Int, Int)] = task1.zip(task2)

  def notParallel =
    for {
      _ <- Task.now(println("Starting"))
      x <- task1
      y <- task2
    } yield x + y

  def parallel =
    for {
      _ <- Task.now(println("Starting"))
      xy <- task3
    } yield xy._1 + xy._2

  Await.result(notParallel.runAsync, Duration.Inf)
  println("-----")
  Await.result(parallel.runAsync, Duration.Inf)
  println("-----")

  private val f1 = future1
  private val f2 = future2

  val f3 = for {
    x <- f1
    y <- f2
  } yield x + y
}
