package de.codecentric.wittig.scala.java.future

import java.util.concurrent.{CompletableFuture, Executors}
import java.util.concurrent.Future

object Main extends App:

  val f1 = CompletableFuture.supplyAsync(() => "x")
  val f2 = CompletableFuture.supplyAsync(() => 42)
  val f3 = f1.thenCompose(v1 => f2.thenApply(v2 => s"Result: $v1 $v2"))

  println(f3.get())

  val completableFuture = CompletableFuture.completedFuture("Hello");
  println(completableFuture.get())

  @throws[InterruptedException]
  def calculateAsync(): Future[String] = {
    val completableFuture: CompletableFuture[String] = new CompletableFuture[String]
    Executors.newCachedThreadPool.submit { () =>
      println("t1")
      Thread.sleep(2000)
      completableFuture.complete("Hallo")
      null
    }
    completableFuture
  }

  val c1 = calculateAsync()
//  val cancelled = c1.cancel(true)
//  println(cancelled)
  println(s"isCancelled: ${c1.isCancelled}")
  println(s"isDone: ${c1.isDone}")
  println(s"get: ${c1.get()}")
  println(s"isDone: ${c1.isDone}")
