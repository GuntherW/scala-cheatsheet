package de.codecentric.wittig.scala.futur

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * @author gunther
 */
object TestFuture extends App {

  def zahl = 3 + 4

  val f = Future(zahl)
  val f2 = Future.successful(zahl)

  f.foreach(println)
  f2.foreach { println }

  println("lkj")
}