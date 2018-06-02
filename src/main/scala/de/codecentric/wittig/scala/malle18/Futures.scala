package de.codecentric.wittig.scala.malle18

import monix.eval.Task

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Futures {
  def saveUser(u: String): Future[Unit] = Future { println("saving user") }

  def saveTransaction(i: Int): Future[Unit] = Future { println("saving transaction") }

  private val eventualUnit: Task[Unit] = Task.defer(Task.fromFuture(saveUser("")))
  private val eventualUnit2: Task[Unit] = Task.fromFuture(saveTransaction(1))

  val result: Task[Unit] = for {
    _ <- eventualUnit
    _ <- eventualUnit2
  } yield ()

  result
}
