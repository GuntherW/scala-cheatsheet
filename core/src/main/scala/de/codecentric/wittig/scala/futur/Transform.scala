package de.codecentric.wittig.scala.futur
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@main
def transform(): Unit =

  val f1    = Future(1)
  val f1Map = f1.map(_ + 1)

  val f1Transform     = f1.transform {
    case Success(value)     => Try(value + 1)
    case Failure(throwable) => Try(0)
  }
  val f1TransformWith = f1.transformWith {
    case Success(value)     => Future(value + 1)
    case Failure(throwable) => Future(0)
  }

  val f2 = f1.flatMap(i => Future(i + 1))

  println(f1Map)           // Future(Success(2))
  println(f1Transform)     // Future(Success(2))
  println(f1TransformWith) // Future(Success(2))
  println(f2) // Future(Success(2))
