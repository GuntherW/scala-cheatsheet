package de.wittig.sttp

import sttp.client4.*
import sttp.client4.jsoniter.*
import com.github.plokhotnyuk.jsoniter_scala.macros.ConfiguredJsonValueCodec

case class Address(street: String, city: String) derives ConfiguredJsonValueCodec
case class PersonalData(name: String, age: Int, address: Address) derives ConfiguredJsonValueCodec
case class HttpBinResponse(origin: String, headers: Map[String, String], data: String) derives ConfiguredJsonValueCodec

object SimpleJson extends App:

  val request = basicRequest
    .post(uri"https://httpbin.org/post")
    .body(asJson(PersonalData("Alice", 25, Address("Marszałkowska", "Warsaw"))))
    .response(asJsonOrFail[HttpBinResponse])

  val backend  = DefaultSyncBackend()
  val response = request.send(backend)

  println(response.code)
  println(response.body)
