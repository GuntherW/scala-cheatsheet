package de.wittig.playground

import scala.util.boundary
import scala.util.boundary.{break, Label}

final class Error[-A](using label: Label[A]) {
  def raise(error: A): Nothing = break(error)
}

type Raise[A] = Error[A] ?=> A

object Raise {

  inline def apply[A](inline body: Error[A] ?=> A): Raise[A] =
    body

  def raise[A](error: A)(using e: Error[A]): Nothing =
    e.raise(error)

  def run[A](raise: Raise[A]): A =
    boundary[A] {
      given error: Error[A] = new Error[A]
      raise
    }
}

object Continuation extends App:

  val program: Raise[String] =
    Raise {
      // This early return is difficult to write in a purely functional style
      List(1, 2, 3, 4)
        .foreach(x => if x == 3 then Raise.raise("Found 3"))
      "No 3 found"
    }

  val result = Raise.run(program)
  println(result)
