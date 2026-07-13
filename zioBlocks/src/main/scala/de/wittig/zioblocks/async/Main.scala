package de.wittig.zioblocks.async

import zio.blocks.async._

@main
def main(): Unit =

  def fetch(id: Int): Async[String] = Async.succeed(s"item-$id")

  val program: Async[Int] =
    Async.async {
      val a = fetch(1).await
      val b = fetch(2).await
      (a + b).length
    }

  println(program)
