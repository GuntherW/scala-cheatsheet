package de.codecentric.wittig.scala.jackson

import com.networknt.schema.SchemaRegistry
import com.networknt.schema.dialect.Dialects

@main
def jsonSchema(): Unit =

  val factory       = SchemaRegistry.withDialect(Dialects.getDraft202012);
  val jsonSchema    = factory.getSchema(getClass.getResourceAsStream("/jsonschema/productSchema.json"))
  val jsonNodeValid = objectMapper.readTree(getClass.getResourceAsStream("/jsonschema/productValid.json"))
  val errorsValid   = jsonSchema.validate(jsonNodeValid)
  println(errorsValid)

  val jsonNodeInvalid = objectMapper.readTree(getClass.getResourceAsStream("/jsonschema/productInvalid.json"))
  val errorsInvalid   = jsonSchema.validate(jsonNodeInvalid)
  println(errorsInvalid)
