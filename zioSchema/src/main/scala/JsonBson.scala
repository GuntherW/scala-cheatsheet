import zio.bson.*
import zio.json.*
import zio.schema.codec.{BinaryCodec, BsonSchemaCodec, ProtobufCodec}
import zio.schema.{DeriveSchema, Schema}

import java.time.LocalDate
import java.util.UUID
import scala.util.chaining.*

object JsonBson extends App:

  final case class Person(name: String, age: Int, date: LocalDate, id: UUID)

  object Person:
    given schema: Schema[Person]       = DeriveSchema.gen[Person]
    given jsonCodec: JsonCodec[Person] = zio.schema.codec.JsonCodec.jsonCodec(schema)
    given bsonCodec: BsonCodec[Person] = BsonSchemaCodec.bsonCodec(Person.schema)

    given jsonBinaryCodec: BinaryCodec[Person] = zio.schema.codec.JsonCodec.schemaBasedBinaryCodec(schema)

    given protobufCodec: BinaryCodec[Person] = ProtobufCodec.protobufCodec

  private val p = Person("John", 30, LocalDate.now, UUID.randomUUID)

  val json       = p.toJson.tap(println)
  val jsonBinary = Person.jsonBinaryCodec.encode(p).tap(println)

  val bson = p.toBsonValue.tap(println)

  val proto = Person.protobufCodec.encode(p)
  assert(Person.protobufCodec.decode(proto) == Right(p))
  Person.protobufCodec.decode(proto).getOrElse("").tap(println)
