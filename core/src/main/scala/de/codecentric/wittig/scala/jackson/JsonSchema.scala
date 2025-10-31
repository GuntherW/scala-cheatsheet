package de.codecentric.wittig.scala.jackson

import com.networknt.schema.SchemaRegistry
import com.networknt.schema.dialect.Dialects

object JsonSchema extends App:

  private val factory       = SchemaRegistry.withDialect(Dialects.getDraft202012);
  private val jsonSchema    = factory.getSchema(getClass.getResourceAsStream("/jsonschema/productSchema.json"))
  private val jsonNodeValid = objectMapper.readTree(getClass.getResourceAsStream("/jsonschema/productValid.json"))
  private val errorsValid   = jsonSchema.validate(jsonNodeValid)
  println(errorsValid)

  private val jsonNodeInvalid = objectMapper.readTree(getClass.getResourceAsStream("/jsonschema/productInvalid.json"))
  private val errorsInvalid   = jsonSchema.validate(jsonNodeInvalid)
  println(errorsInvalid)
