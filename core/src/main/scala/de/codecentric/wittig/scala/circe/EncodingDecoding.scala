package de.codecentric.wittig.scala.circe
import io.circe
import io.circe.Decoder.Result
import io.circe.Json
import io.circe.parser.decode
import io.circe.syntax._

object EncodingDecoding extends App {
  simple()
  semiAutomatic()
  withAnnotation()
  automatic()
  custom()
  custom2PiggyBack()

  def simple(): Unit = {
    val intsJson: Json                     = List(1, 2, 3).asJson           // encoding
    val l: Result[List[Int]]               = intsJson.as[List[Int]]         // decoding
    val l2: Either[circe.Error, List[Int]] = decode[List[Int]]("[1, 2, 3]") // using directly
    println(intsJson)
    println(l)
    println(l2)
  }

  def semiAutomatic(): Unit = {
    import io.circe._
    import io.circe.generic.semiauto._

    case class Foo(a: Int, b: String, c: Boolean)

    implicit val fooDecoder: Decoder[Foo] = deriveDecoder[Foo]
    implicit val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]

    val foo  = Foo(1, "22", true)
    val json = foo.asJson
    val foo2 = decode[Foo](json.toString)
    println(json)
    println(foo2)
  }

  // Macro Paradise needed
  def withAnnotation(): Unit = {
    import io.circe.generic.JsonCodec
    import io.circe.syntax._

    @JsonCodec case class Bar(i: Int, s: String)

    val json = Bar(13, "Qux").asJson
    println(json)
  }

  def automatic(): Unit = {
    import io.circe.generic.auto._
    import io.circe.syntax._

    case class Person(name: String)
    case class Greeting(salutation: String, person: Person, exclamationMarks: Int)

    val json = Greeting("Hey", Person("Chris"), 3).asJson
    println(json)
  }

  def custom(): Unit = {
    import io.circe.{Decoder, Encoder, HCursor, Json}

    case class Thing(foo: String, bar: Int)

    implicit val encodeThing: Encoder[Thing] = new Encoder[Thing] {
      final def apply(a: Thing): Json =
        Json.obj(
          ("fu", Json.fromString(a.foo)),
          ("bah", Json.fromInt(a.bar))
        )
    }

    implicit val decodeThing: Decoder[Thing] = new Decoder[Thing] {
      final def apply(c: HCursor): Decoder.Result[Thing] =
        for {
          foo <- c.downField("fu").as[String]
          bar <- c.downField("bah").as[Int]
        } yield Thing(foo, bar)
    }
    val thing                                = Thing("foo/fu", 4)
    println(thing.asJson)
    println(decode[Thing](thing.asJson.toString))
  }

  // piggyback on top of the decoders that are already available.
  def custom2PiggyBack(): Unit = {
    import java.time.Instant

    import cats.syntax.either._
    import io.circe.{Decoder, Encoder}

    implicit val encodeInstant: Encoder[Instant] = Encoder.encodeString.contramap[Instant](_.toString)

    implicit val decodeInstant: Decoder[Instant] = Decoder.decodeString.emap { str =>
      Either.catchNonFatal(Instant.parse(str)).leftMap(t => "Instant")
    }

    val instant = Instant.now()
    println(instant.asJson)
    println(decode[Instant](instant.asJson.toString))
  }
}
