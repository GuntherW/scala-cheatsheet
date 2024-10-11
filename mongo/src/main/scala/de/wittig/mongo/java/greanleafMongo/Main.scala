package de.wittig.mongo.java.greanleafMongo

import java.time.ZonedDateTime

import io.circe.Codec
import io.circe.syntax.EncoderOps
import io.circe.generic.semiauto.deriveCodec
import io.github.greenleafoss.mongo.circe.bson.CirceBsonProtocol
import io.github.greenleafoss.mongo.circe.json.CirceJsonProtocol
import org.bson.types.ObjectId
import io.github.greenleafoss.mongo.core.util.ZonedDateTimeOps.parseZonedDateTime

object Main extends App:

  final case class Test(_id: ObjectId, i: Int, l: Long, b: Boolean, zdt: ZonedDateTime)

  // JSON
  trait TestJsonProtocol extends CirceJsonProtocol:
    given testJsonFormat: Codec[Test] = deriveCodec

  object TestJsonProtocol extends TestJsonProtocol

  // BSON
  object TestBsonProtocol extends TestJsonProtocol with CirceBsonProtocol

  // Circe JSON

  val obj = Test(new ObjectId("5c72b799306e355b83ef3c86"), 1, 0x123456789L, true, "1970-01-01T00:00:00Z".parseZonedDateTime)

  import TestJsonProtocol.given
  println(obj.asJson)

  import TestBsonProtocol.given
  println(obj.asJson)
