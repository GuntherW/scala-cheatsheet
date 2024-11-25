package de.wittig.mongo.greanleafMongo

import java.time.ZonedDateTime

import de.wittig.MongoUtil.mongoClient
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import io.github.greenleafoss.mongo.circe.bson.CirceBsonProtocol
import io.github.greenleafoss.mongo.circe.json.CirceJsonProtocol
import io.github.greenleafoss.mongo.core.util.ZonedDateTimeOps.parseZonedDateTime
import org.mongodb.scala.bsonDocumentToUntypedDocument

object Main extends App:
  import TestBsonProtocol.given

  val obj = Test(1, 0x123456789L, true, "1970-01-01T00:00:00Z".parseZonedDateTime)

  try {
    val database   = mongoClient.getDatabase("test1")
    val collection = database.getCollection("greanleaf")
    collection.drop()

    collection.insertOne(obj.asDocument())
    val doc = collection.find().first()
    println(doc.toBsonDocument)
  } finally if (mongoClient != null) mongoClient.close()

final case class Test(i: Int, l: Long, b: Boolean, zdt: ZonedDateTime)

object TestBsonProtocol extends CirceJsonProtocol with CirceBsonProtocol:
  given testJsonFormat: Codec[Test] = deriveCodec
