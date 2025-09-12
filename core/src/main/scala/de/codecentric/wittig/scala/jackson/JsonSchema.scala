package de.codecentric.wittig.scala.jackson

import com.networknt.schema.{JsonSchemaFactory, SpecVersion}

object JsonSchema extends App:

  private val factory       = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
  private val jsonSchema    = factory.getSchema(getClass.getResourceAsStream("/jsonschema/productSchema.json"))
  private val jsonNodeValid = objectMapper.readTree(getClass.getResourceAsStream("/jsonschema/productValid.json"))
  private val errorsValid   = jsonSchema.validate(jsonNodeValid)
  println(errorsValid)

  private val jsonNodeInvalid = objectMapper.readTree(getClass.getResourceAsStream("/jsonschema/productInvalid.json"))
  private val errorsInvalid   = jsonSchema.validate(jsonNodeInvalid)
  println(errorsInvalid)
