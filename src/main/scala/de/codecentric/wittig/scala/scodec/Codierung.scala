package de.codecentric.wittig.scala.scodec

import scodec.Codec
import scodec.codecs.implicits._
/**
 * @author gunther
 */
object Codierung extends App {
  case class Point(x: Int, y: Int)
  case class Line(start: Point, end: Point)
  case class Arrangement(arr: Vector[Line])

  val arr = Arrangement(Vector(Line(Point(1, 2), Point(3, 4)), Line(Point(5, 6), Point(1, 3))))

  println(arr)
  println("---")

  val a = Codec.encode(arr).require
  println(a)
  println("---")

  val arr2 = Codec[Arrangement].decode(a).require
  println(arr2)
  println("---")

  // durch zlib jagen:
  import scodec.codecs.zlib
  val compressed = zlib(Codec[Arrangement])
  println(compressed.encode(arr))
  println("---")

}