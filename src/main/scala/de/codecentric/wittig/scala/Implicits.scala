package de.codecentric.wittig.scala
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.duration._

object Implicits {

  implicit class RichFuture[T](future: Future[T]) {
    def await(implicit duration: Duration = 10 seconds): T = Await.result(future, duration)
  }

}
