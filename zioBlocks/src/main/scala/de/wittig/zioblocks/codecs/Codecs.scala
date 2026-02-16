package de.wittig.zioblocks.codecs

import de.wittig.zioblocks.Person
import zio.blocks.schema.Schema
import zio.blocks.schema.avro.AvroFormat
import zio.blocks.schema.bson.BsonSchemaCodec
import zio.blocks.schema.json.JsonFormat
import zio.blocks.schema.msgpack.MessagePackFormat
import zio.blocks.schema.thrift.ThriftFormat
import zio.blocks.schema.toon.ToonFormat

@main
def main(): Unit =

  val jsonCodec    = Schema[Person].derive(JsonFormat)        // JSON
  val avroCodec    = Schema[Person].derive(AvroFormat)        // Avro
  val toonCodec    = Schema[Person].derive(ToonFormat)        // TOON (LLM-optimized)
  val msgpackCodec = Schema[Person].derive(MessagePackFormat) // MessagePack
  val thriftCodec  = Schema[Person].derive(ThriftFormat)      // Thrift
  val bsonCodec    = BsonSchemaCodec.bsonCodec(Schema[Person])

  val max = Person("Max", 42)
  println(jsonCodec.encodeToString(max))
  println(toonCodec.encodeToString(max))
  println(bsonCodec.encoder.toBsonValue(max))
