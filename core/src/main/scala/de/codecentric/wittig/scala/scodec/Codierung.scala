package de.codecentric.wittig.scala.scodec

import scodec.codecs.implicits._
import scodec._
import scodec.bits._
import codecs._
import de.codecentric.wittig.scala.Printer.printlnYellow

/**
  * @author gunther
  */
object Codierung extends App {

  printlnYellow("encodeDecode")
  encodeDecode()

  printlnYellow("combinator")
  combinator()

  printlnYellow("caseClassBinding")
  caseClassBinding()

  printlnYellow("scodecDoku")
  scodecDoku()

  def encodeDecode(): Unit = {

    case class Point(x: Int, y: Int)
    case class Line(start: Point, end: Point)
    case class Arrangement(arr: Vector[Line])

    val arr = Arrangement(Vector(Line(Point(1, 2), Point(3, 4)), Line(Point(5, 6), Point(1, 3))))
    println(arr)

    val encoded = Codec.encode(arr).require
    println(encoded)

    val decoded = Codec[Arrangement].decode(encoded).require
    println(decoded)

    // durch zlib jagen:
    import scodec.codecs.zlib
    val compression = zlib(Codec[Arrangement])
    val compressed  = compression.encode(arr).require
    println(compressed)
    println(compression.decode(compressed).require)
  }

  def combinator(): Unit = {
    // Create a codec for an 8-bit unsigned int followed by an 8-bit unsigned int followed by a 16-bit unsigned int
    val firstCodec = uint8 ~ uint8 ~ uint16

    // Decode a bit vector using that codec
    val result: Attempt[DecodeResult[Int ~ Int ~ Int]] = firstCodec.decode(hex"102a03ff".bits)
    println(result) // Successful(DecodeResult(((16, 42), 1023), BitVector(empty)))

    // Sum the result
    val add3                            = (_: Int) + (_: Int) + (_: Int)
    val sum: Attempt[DecodeResult[Int]] = result.map(_.map(add3))
    println(sum) // Successful(DecodeResult(1081, BitVector(empty)))
  }

  def caseClassBinding(): Unit = {
    case class Point(x: Int, y: Int, z: Int)

    val pointCodec: Codec[Point] = (int8 :: int8 :: int8).as[Point]

    val encoded: Attempt[BitVector] = pointCodec.encode(Point(-5, 10, 1))
    println(encoded) // Successful(BitVector(24 bits, 0xfb0a01))

    val decoded: Attempt[DecodeResult[Point]] = pointCodec.decode(encoded.require)
    println(decoded) // Successful(DecodeResult(Point(-5, 10, 1), BitVector(empty)))
  }

  def scodecDoku(): Unit = {
    val x = hex"deadbeef"
    val y = hex"DEADBEEF"
    println(x)
    println(y)
    println(bin"00110100")
    println(bin"00110100".bytes)
    println(bin"00110110101".bytes)
    println(bin"001101101001")

    case class Point(x: Int, y: Int)

    val tupleCodec: Codec[(Int, Int)] = uint8 ~ uint8
//    val pointCodec: Codec[Point]      = tupleCodec.widenOpt(Point.apply, Point.unapply)
  }
}
