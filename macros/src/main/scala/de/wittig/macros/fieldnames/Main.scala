package de.wittig.macros.fieldnames

import scala.compiletime.{constValue, erasedValue, error, summonFrom}
import scala.deriving.*

object Main extends App:

  inline def fieldNames[T]: List[String] =
    summonFrom {
      case m: Mirror.ProductOf[T] => literalStrings[m.MirroredElemLabels]
      case _                      => error("Not a case class")
    }

  inline def literalStrings[T]: List[String] =
    inline erasedValue[T] match
      case _: (head *: tail) => constValue[head].toString :: literalStrings[tail]
      case EmptyTuple        => Nil

  fieldNames[Test].foreach(println)

case class Test(x: Int, y: String):
  val z: Int           = 5
  def sayHallo: String = "hallo"
