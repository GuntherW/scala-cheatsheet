package de.codecentric.wittig.scala.monads

import cats.data.Writer
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxWriterId}
import cats.catsInstancesForId

object WriterMonadExample extends App:
  type Logged[A] = Writer[Vector[String], A]

  val direct1: Logged[Int]  = 123.pure[Logged]
  val direct2: Logged[Unit] = Vector("msg1", "msg2").tell
  val direct3: Logged[Int]  = Writer(Vector("msg1", "msg2"), 123) // 123.writer(Vector("msg1", "mgs2"))

  println(direct1.value)
  println(direct2.written)
  println(direct3.run)

  val example =
    for
      a <- 10.pure[Logged]
      _ <- Vector("a", "b").tell
      b <- 32.writer(Vector("c"))
    yield a + b
  println(example.run)

  println(example.mapWritten(_.map(_.toUpperCase)))
  println(example.map(_ * 100))
  println(example.bimap(
    log => log.map(_.toUpperCase),
    res => res * 1000
  ))
