package de.codecentric.wittig.scala.isinstanceofAntiPattern

import de.codecentric.wittig.scala.Ops.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/** https://alexn.org/blog/2019/08/11/isinstanceof-anti-pattern.html
  */
object Main extends App:

  guarantee {
    println("hallo welt")
    1 + 1
  } {
    println("fertig!")
  }

  guarantee {
    Future {
      println("Executing!")
      1 + 1
    }
  } {
    println("Done!")
  }.await

  def guarantee[R](f: => R)(finalizer: => Unit): R =
    try f
    finally finalizer
