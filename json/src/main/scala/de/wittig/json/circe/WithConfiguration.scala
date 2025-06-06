package de.wittig.json.circe

import io.circe.Decoder
import io.circe.derivation.Configuration
import io.circe.parser.*

object WithConfiguration extends App:

  case class Foo(a: String = "Standard", b: Int = 123)

  object Foo:
    given Configuration = Configuration.default.withDefaults
    given Decoder[Foo]  = Decoder.derivedConfigured

  println(decode[Foo]("""{}"""))
  println(decode[Foo]("""{ "a": "bar" }"""))
  println(decode[Foo]("""{ "a": "bar", "b": 1 }"""))
