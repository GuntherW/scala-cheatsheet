package de.codecentric.wittig.scala.refined

import cats.implicits._
import de.codecentric.wittig.scala.refined.Implicits._
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric._
import eu.timepit.refined.types.numeric.PosInt

//final case class Person(age: Int Refined Positive)
final case class Person(age: PosInt)

/**
  * refineV for Runtime (V steht für Value)
  * refineMV for Compiletime (MV steht für MacroValue)
  */
object Main extends App {

  val good: Either[String, Refined[Int, Positive]] = refineV[Positive](5)
  val bad: Either[String, Int Refined Positive]    = refineV[Positive](-5).leftMap(_.red)
  println(good)
  println(bad)

  val a1 = refineMV[Positive](5)
  //  val a2 = refineMV[Positive](-5) // wont compile MV for "MacroValue"

  val p1 = Person(refineMV(5))
  println(p1.age.value.toString.cyan)

  val p2 = Person(5)
//  val p3 = Person(-5)

  println(PosInt.from(5))
  println(PosInt.from(-5))
}

object Implicits {
  implicit class StringColoured(s: String) {
    def red: String    = Console.RED + s + Console.RESET
    def cyan: String   = Console.CYAN + s + Console.RESET
    def blue: String   = Console.BLUE + s + Console.RESET
    def yellow: String = Console.YELLOW + s + Console.RESET
  }
}
