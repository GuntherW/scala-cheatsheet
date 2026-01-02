package de.codecentric.wittig.scala.jackson

import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.scala.DefaultScalaModule
import com.networknt.schema.{Schema, SchemaRegistry}
import com.networknt.schema.dialect.Dialects
import JsonSchema.*

import scala.util.chaining.scalaUtilChainingOps

@main
def schema(): Unit =

  jsonSchema.validate(jsonNodeValid).tap(println)
  jsonSchema.validate(jsonNodeInvalid).tap(println)

object JsonSchema:
  private val mapper = JsonMapper.builder()
    .addModule(DefaultScalaModule)
    .build()
  
  private val factory: SchemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012)
  val jsonSchema: Schema              = factory.getSchema(getClass.getResourceAsStream("/jsonschema/productSchema.json"))
  val jsonNodeValid: JsonNode         = mapper.readTree(getClass.getResourceAsStream("/jsonschema/productValid.json"))
  val jsonNodeInvalid: JsonNode       = mapper.readTree(getClass.getResourceAsStream("/jsonschema/productInvalid.json"))
