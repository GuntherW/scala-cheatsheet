package de.codecentric.wittig.scala.zio.minizio

/**
  * https://gist.github.com/jdegoes/9729a8e50b2fd2f473a4ee9371755134#file-mini-zio-scala-L17
  */
final case class ZIO[-R, +E, +A](run: R => Either[E, A]) {

  def map[B](f: A => B): ZIO[R, E, B] =
    ZIO(r => run(r).map(f))

  def flatMap[R1 <: R, E1 >: E, B](f: A => ZIO[R1, E1, B]): ZIO[R1, E1, B] =
    ZIO(r => run(r).flatMap(a => f(a).run(r)))

  def provide(r: R): ZIO[Any, E, A] =
    ZIO(_ => run(r))

  def either: ZIO[R, Nothing, Either[E, A]] =
    ZIO(r => Right(run(r)))
}

object ZIO {
  def succeed[A](a: A): ZIO[Any, Nothing, A] = ZIO(_ => Right(a))
  def fail[E](e: E): ZIO[Any, E, Nothing]    = ZIO(_ => Left(e))
  def environment[R]: ZIO[R, Nothing, R]     = ZIO(r => Right(r))
  def effect[A](action: => A): ZIO[Any, Throwable, A] = ZIO { _ =>
    try Right(action)
    catch {
      case t: Throwable => Left(t)
    }
  }
}
