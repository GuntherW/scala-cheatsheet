package de.codecentric.wittig.scala.malle18

import cats.Monoid
import cats.data.Validated._
import cats.data.{NonEmptyList, OptionT, Validated, ValidatedNel}
import cats.instances.future._
import cats.instances.int._
import cats.instances.list._
import cats.instances.option._
import cats.instances.set._
import cats.instances.string._
import cats.instances.unit._
import cats.kernel.Eq
import cats.syntax.eq._
import cats.syntax.monoid._
import cats.syntax.option.catsSyntaxOptionId
import cats.syntax.traverse._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Folds {
  val result = List(1, 2, 3).foldLeft(0)(_ + _)

  def combineAll[A: Monoid](xs: List[A]): A = {
    xs.foldLeft(Monoid[A].empty)(Monoid[A].combine)
  }

  combineAll(List(1, 2, 3))
  combineAll(List("a", "b", "c"))
  combineAll(List(Set(1, 2, 3), Set(4, 5, 6)))

  val ffxs: List[Future[Int]] = List(1, 2, 3).map(x => Future(x + 1))

  val fxs: Future[List[Int]] = Future.traverse(List(1, 2, 3))(x => Future(x + 1))

  val r: Option[List[Int]] = List(1, 2, 3).traverse(x => 1.some)

  NonEmptyList.of(1)

  def validate1(s: String): ValidatedNel[String, Unit] = if (s.length > 5) Validated.Valid(()) else Validated.invalidNel("too short")
  def validate2(s: String): ValidatedNel[String, Unit] = if (s.length > 5) Validated.Valid(()) else Validated.invalidNel("number missing")
  def validate3(s: String): ValidatedNel[String, Unit] = if (s.length > 5) Validated.Valid(()) else Validated.invalidNel("at least one special char")

  def validateAll(s: String): Either[NonEmptyList[String], Unit] = {
    val result = validate1(s) |+| validate2(s) |+| validate3(s)

    result.toEither
  }

  val x: Future[Option[Int]] = (for {
    x <- OptionT[Future, Int](Future(1.some))
    y <- OptionT[Future, Int](Future((x + 1).some))
    z <- OptionT.liftF(Future(y + 1))
    a <- OptionT.pure[Future](z + 1)
    b <- OptionT.some[Future](a + 1)
  } yield y).value

  val userId: UserId = UserId("123")

  val someCode = UserId("123") === userId
}

case class UserId(s: String) extends AnyVal

object UserId {
  implicit val userEq: Eq[UserId] = Eq.fromUniversalEquals
}
