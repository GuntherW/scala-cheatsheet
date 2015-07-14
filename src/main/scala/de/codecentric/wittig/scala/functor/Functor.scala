package de.codecentric.wittig.scala.functor

import simulacrum._
import scala.language.higherKinds
import scala.language.implicitConversions

/**
 *
 * In der console:
 *
 *
 *
 * @author gunther
 */

trait Functor[F[_]] {
  def map[A, B](fa: F[A])(f: A => B): F[B]
}

trait FunctorLaws {
  def identity[F[_], A](fa: F[A])(implicit F: Functor[F]) = {
    F.map(fa)(a => a) == fa
  }

  def composition[F[_], A, B, C](fa: F[A], f: A => B, g: B => C)(implicit F: Functor[F]) = {
    F.map(F.map(fa)(f))(g) == F.map(fa)(f andThen (g))
  }
}

object Functor {
  implicit val listFunctor: Functor[List] = new Functor[List] {
    def map[A, B](fa: List[A])(f: A => B): List[B] = fa.map(f)
  }
  implicit val listOption: Functor[Option] = new Functor[Option] {
    def map[A, B](fa: Option[A])(f: A => B): Option[B] = fa.map(f)
  }

  //  implicit def function1Functor[X]: Functor[X => ?] = new Functor[X => ?] {
  //    def map[A, B](fa: X => A)(f: A => B): X => B = fa andThen f
  //  }

  /**
   * import de.codecentric.wittig.scala.functor._
   * implicitly[Functor[List]]
   * res0.map(List(1,2,3,4,5))(_+1)
   */
}
