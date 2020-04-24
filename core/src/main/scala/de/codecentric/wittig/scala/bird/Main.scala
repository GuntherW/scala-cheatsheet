package de.codecentric.wittig.scala.bird
import scala.util.chaining._

/**
  * @see https://leobenkel.com/2019/10/bird-operator-in-scala/
  */
object Main extends App {
  val zahl = "-5"
  val erg  = zahl.pipe(toI).pipe(scala.math.abs).pipe(toS)

  val list = List(1, 2, 3)

  println(erg)

  def toI(s: String) = s.toInt
  def toS(i: Int)    = i.toString
}
