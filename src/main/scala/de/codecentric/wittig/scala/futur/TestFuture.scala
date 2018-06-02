package de.codecentric.wittig.scala.futur

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success

/**
  * @author gunther
  */
object TestFuture extends App {

  val f = Future { 5 }
  f andThen {
    case _ =>
      println("gunther")
      throw new IllegalArgumentException("dd")
  } andThen {
    case Failure(t) => println(t)
    case Success(v) => println(v)
  }

  Thread.sleep(1000)
}
