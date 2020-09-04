package de.wittig.zio

import cats.implicits.none
import zio.{App, Task}

import scala.io.StdIn

object MainTask extends App {
  def run(args: List[String]) =
    myAppLogic.either.map {
      _.fold(t => { println(t); 1 }, _ => 0)
    }.exitCode

  private val myAppLogic =
    for {
      nameVar <- Task.fromEither(none[String].toRight(new Exception("option was null")))
      _       <- Task(println(s"Was ist Dein $nameVar?"))
      n       <- Task(StdIn.readLine(""))
      _       <- Task(println(s"Hello, ${n}, good to meet you!"))
    } yield ()
}
