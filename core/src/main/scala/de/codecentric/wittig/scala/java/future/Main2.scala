package de.codecentric.wittig.scala.java.future

import java.util.concurrent.CompletableFuture

import de.codecentric.wittig.scala.Ops.time

object Main2 extends App:

  private def createMaterial(name: String, gewicht: Int) =
    Thread.sleep(2000)
    Material(name, gewicht)

  case class Material(name: String, gewicht: Int)

  time {
    val f1 = CompletableFuture.supplyAsync(() => createMaterial("Stein", 1))
    val f2 = CompletableFuture.supplyAsync(() => createMaterial("Stein", 2))
    val f3 = CompletableFuture.supplyAsync(() => createMaterial("Stein", 3))

    val all = CompletableFuture.allOf(f1, f2, f3)
    all.join() // wait for all tasks to complete (oder all.get()??)
    println(f1.get())
    println(f2.get())
    println(f3.get())

  }
