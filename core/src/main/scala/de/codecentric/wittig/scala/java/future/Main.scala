package de.codecentric.wittig.scala.java.future

import java.util.concurrent.CompletableFuture

object Main extends App:

  val f1 = CompletableFuture.supplyAsync(() => "x")
  val f2 = CompletableFuture.supplyAsync(() => 42)
  val f3 = f1.thenCompose(v1 => f2.thenApply(v2 => s"Result: $v1 $v2"))

  println(f3.get())
