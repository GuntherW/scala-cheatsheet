package de.codecentric.wittig.scala.functor

import simulacrum._
import scala.language.higherKinds
import scala.language.implicitConversions

/**
 * @author gunther
 */
@typeclass trait Applicative[F[_]] {
  def pure[A](a: A): F[A]

  def apply[A, B](fa: F[A], ff: F[A => B]): F[B]
  def map[A, B](fa: F[A])(ff: A => B): F[B] =
    apply(fa, pure(ff))
}

object Applicative {
  implicit val optionApplicative: Applicative[Option] = new Applicative[Option] {
    def pure[A](a: A): Option[A] = Some(a)
    def apply[A, B](fa: Option[A], ff: Option[A => B]): Option[B] = (fa, ff) match {
      case (None, _) => None
      case (Some(a), None) => None
      case (Some(a), Some(f)) => Some(f(a))
    }
  }
  implicit val listApplicative: Applicative[List] = new Applicative[List] {
    def pure[A](a: A): List[A] = List(a)
    def apply[A, B](fa: List[A], ff: List[A => B]): List[B] = {
      (fa zip ff).map { case (a, f) => f(a) }
    }
  }
}
