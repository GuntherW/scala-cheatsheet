package de.codecentric.wittig.scala.circe
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

  def simple() = {
    val intsJson: Json       = List(1, 2, 3).asJson           // encoding
    val l: Result[List[Int]] = intsJson.as[List[Int]]         // decoding
    val l2                   = decode[List[Int]]("[1, 2, 3]") // using directly
    println(intsJson)
  }

  def semiAutomatic() = {
    import io.circe._
    import io.circe.generic.semiauto._

    case class Foo(a: Int, b: String, c: Boolean)

    implicit val fooDecoder: Decoder[Foo] = deriveDecoder[Foo]
    implicit val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]

    val foo  = Foo(1, "22", true)
    val json = foo.asJson
    println(json)
  }

  // Macro Paradise needed
  def withAnnotation() = {
    import io.circe.generic.JsonCodec
    import io.circe.syntax._

    @JsonCodec case class Bar(i: Int, s: String)

    val json = Bar(13, "Qux").asJson
    println(json)
  }

  def automatic() = {
    import io.circe.generic.auto._
    import io.circe.syntax._

    case class Person(name: String)

    case class Greeting(salutation: String, person: Person, exclamationMarks: Int)

    val json = Greeting("Hey", Person("Chris"), 3).asJson
    println(json)
  }

  def custom() = {
    import io.circe.{Decoder, Encoder, HCursor, Json}

    class Thing(val foo: String, val bar: Int)

    implicit val encodeFoo: Encoder[Thing] = new Encoder[Thing] {
      final def apply(a: Thing): Json = Json.obj(
        ("foo", Json.fromString(a.foo)),
        ("bar", Json.fromInt(a.bar))
      )
    }

    implicit val decodeFoo: Decoder[Thing] = new Decoder[Thing] {
      final def apply(c: HCursor): Decoder.Result[Thing] =
        for {
          foo <- c.downField("foo").as[String]
          bar <- c.downField("bar").as[Int]
        } yield {
          new Thing(foo, bar)
        }
    }
  }

  // piggyback on top of the decoders that are already available.
  def custom2PiggyBack() = {
    import java.time.Instant

    import cats.syntax.either._
    import io.circe.{Decoder, Encoder}

    implicit val encodeInstant: Encoder[Instant] = Encoder.encodeString.contramap[Instant](_.toString)

    implicit val decodeInstant: Decoder[Instant] = Decoder.decodeString.emap { str =>
      Either.catchNonFatal(Instant.parse(str)).leftMap(t => "Instant")
    }
  }
}
