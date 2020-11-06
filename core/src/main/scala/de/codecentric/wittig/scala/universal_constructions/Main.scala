package de.codecentric.wittig.scala.universal_constructions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Main extends App {

  val l = List(1, 3, 3)

  def left[A](a: A): Either[A, Nothing]  = Left(a)
  def right[A](a: A): Either[Nothing, A] = Right(a)

  def first[A, B](x: (A, B)): A  = x._1
  def second[A, B](x: (A, B)): B = x._2

  // For Product Types:
  def fanOut[C, A, B](f: C => A, g: C => B): C => (A, B) =
    c => (f(c), g(c))

  def fanIn[C, A, B](h: C => (A, B)): (C => A, C => B) =
    (h(_)._1, h(_)._2)

  def bimap[A, A1, B, B1](f: A => A1, g: B => B1): ((A, B)) => (A1, B1) =
    fanOut(f compose (_._1), g compose (_._2))

  // For Sum Types
  def either[C, A, B](f: A => C, g: B => C): Either[A, B] => C = {
    case Left(a)  => f(a)
    case Right(b) => g(b)
  }

  val f = Future(1)

  //def choice[A, A1, B, B1](f: A => A1, g: B => B1): Either[A,B] =>
}
