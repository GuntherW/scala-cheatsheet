package de.codecentric.wittig.scala
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}

object Implicits {
  implicit class RichFuture[T](future: Future[T]) {
    def await(implicit duration: Duration = 10 seconds): T = Await.result(future, duration)
  }

  def time[A](a: => A) = {
    val now    = System.nanoTime
    val result = a
    val micros = (System.nanoTime - now) / 1000000L
    println("%d Mikrosekunden".format(micros))
    result
  }
}
