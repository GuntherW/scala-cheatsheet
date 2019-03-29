package de.codecentric.wittig.scala.futur

import cats.syntax.option.catsSyntaxOptionId

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success
import de.codecentric.wittig.scala.Implicits._

/**
  * @author gunther
  */
object TestFuture extends App {

//  test1

  def test1 = {

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
}
