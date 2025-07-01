package de.wittig.macros.inlines

import scala.compiletime.{constValue, erasedValue, error, summonFrom}
import scala.deriving.*

object FieldNames extends App:

  inline def fieldNames[T]: List[String] =
    summonFrom {
      case m: Mirror.ProductOf[T] => literalStrings[m.MirroredElemLabels]
      case _                      => error("Not a case class")
    }

  inline def fieldNames2[T](using m: Mirror.ProductOf[T]): List[String] =
    literalStrings[m.MirroredElemLabels]

  inline def literalStrings[T <: Tuple]: List[String] =
    inline erasedValue[T] match
      case _: (head *: tail) => constValue[head].toString :: literalStrings[tail]
      case EmptyTuple        => Nil

  println(fieldNames[Test])
  println(fieldNames2[Test])

case class Test(x: Int, y: String)
