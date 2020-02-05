package de.codecentric.wittig.scala.catsRelated.Eval
import cats.Eval

object Main extends App {
  println("Hallo Welt")
  println("Hallo Welt")

  val now    = Eval.now(math.random + 1000)
  val always = Eval.always(math.random + 3000)
  val later  = Eval.later(math.random + 2000)

  println(now.value)
}
