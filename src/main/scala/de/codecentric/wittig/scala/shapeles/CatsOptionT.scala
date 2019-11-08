package de.codecentric.wittig.scala.shapeles

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import cats.data.OptionT
import cats.implicits._

object CatsOptionT extends App {
  def futureNone: Future[Option[String]] = Future.successful(None)

  def greetingFO: Future[Option[String]] = Future.successful(Some("Hello"))

  def firstnameF: Future[String] = Future.successful("Jane")

  def lastnameO: Option[String] = Some("Doe")

  val ot: OptionT[Future, String] = for {
    g <- OptionT(greetingFO)
    f <- OptionT.liftF(firstnameF)
    l <- OptionT.fromOption[Future](lastnameO)
    m <- OptionT(futureNone).orElse(OptionT(greetingFO))
  } yield s"$g $f $l $m"

  val result: Future[Option[String]] = ot.value // Future(Some("Hello Jane Doe"))
}
