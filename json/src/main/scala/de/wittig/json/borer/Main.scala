package de.wittig.json.borer
import io.bullet.borer.Cbor

object Main extends App:

  val value = List("foo", "bar", "baz") // example value

  val bytes: Array[Byte] = Cbor.encode(value).toByteArray // throws on error
  println(bytes)

  val list: List[String] = Cbor.decode(bytes).to[List[String]].value // throws on error
  println(list)