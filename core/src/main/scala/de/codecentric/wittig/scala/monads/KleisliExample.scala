package de.codecentric.wittig.scala.monads

import cats.data.Kleisli
import cats.implicits.*

/** Kleisli enables composition of functions that return a monadic value
  *
  * @see
  *   https://typelevel.org/cats/datatypes/kleisli.html
  */
object KleisliExample extends App {

  val parse: Kleisli[Option, String, Int] =
    Kleisli(s => if (s.matches("-?[0-9]+")) s.toInt.some else None)

  val reciprocal: Kleisli[Option, Int, Double] =
    Kleisli(i => if (i != 0) Some(1.0 / i) else None)

  val parseAndReciprocal: Kleisli[Option, String, Double] = {
//    parse.andThen(reciprocal)
    reciprocal.compose(parse)
  }

  println(parseAndReciprocal.run("3"))
}
