package com.example.observable

import scala.concurrent.duration._
import rx.lang.scala.Observable

import scala.language.postfixOps
import io.StdIn._

object Obs extends App {

  val ticks = Observable.interval(1 seconds)
  val evens = ticks.filter { _ % 2 == 0 }
  val bufs = evens.slidingBuffer(3, 2)

  val subscribtion = bufs.subscribe(println(_))

  readLine()

  subscribtion.unsubscribe()

}
