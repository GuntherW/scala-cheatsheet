package de.codecentric.wittig.scala.shapeles

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import cats.data.OptionT
import cats.implicits._

object CatsOptionT extends App {

  val greetingFO: Future[Option[String]] = Future.successful(Some("Hello"))

  val firstnameF: Future[String] = Future.successful("Jane")

  val lastnameO: Option[String] = Some("Doe")

  val ot: OptionT[Future, String] = for {
    g <- OptionT(greetingFO)
    f <- OptionT.liftF(firstnameF)
    l <- OptionT.fromOption[Future](lastnameO)
  } yield s"$g $f $l"

  val result: Future[Option[String]] = ot.value // Future(Some("Hello Jane Doe"))

}
