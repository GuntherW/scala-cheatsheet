package de.codecentric.wittig.scala.circe
import io.circe.*
import io.circe.parser.*

object TraversingAndModifying extends App:
  val json: String = """
  {
    "id": "c730433b-082c-4984-9d66-855c243266f0",
    "name": "Foo",
    "counts": [1, 2, 3],
    "values": {
      "bar": true,
      "baz": 100.001,
      "qux": ["a", "b"]
    }
  }
"""

  val doc: Json = parse(json).getOrElse(Json.Null)

  // Curser needed
  val cursor: HCursor = doc.hcursor

  val baz: Decoder.Result[Double] = cursor.downField("values").downField("baz").as[Double]
  // baz: io.circe.Decoder.Result[Double] = Right(100.001)

  // You can also use `get[A](key)` as shorthand for `downField(key).as[A]`
  val baz2: Decoder.Result[Double] = cursor.downField("values").get[Double]("baz")
  // baz2: io.circe.Decoder.Result[Double] = Right(100.001)

  val secondQux: Decoder.Result[String] = cursor.downField("values").downField("qux").downArray.right.as[String]
  // secondQux: io.circe.Decoder.Result[String] = Right(b)

  // Transforming data:
  val reversedNameCursor: ACursor = cursor.downField("name").withFocus(_.mapString(_.reverse))
  val reversedName: Option[Json]  = reversedNameCursor.top // The result contains the original document with the "name" field reversed.
