package de.codecentric.wittig.scala.jackson

import com.fasterxml.jackson.databind.JsonNode
import com.networknt.schema.{Schema, SchemaRegistry}
import com.networknt.schema.dialect.Dialects
import JsonSchema.*

import scala.util.chaining.scalaUtilChainingOps

@main
def schema(): Unit =

  jsonSchema.validate(jsonNodeValid).tap(println)
  jsonSchema.validate(jsonNodeInvalid).tap(println)

object JsonSchema:
  private val factory: SchemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012);
  val jsonSchema: Schema              = factory.getSchema(getClass.getResourceAsStream("/jsonschema/productSchema.json"))
  val jsonNodeValid: JsonNode         = objectMapper.readTree(getClass.getResourceAsStream("/jsonschema/productValid.json"))
  val jsonNodeInvalid: JsonNode       = objectMapper.readTree(getClass.getResourceAsStream("/jsonschema/productInvalid.json"))
