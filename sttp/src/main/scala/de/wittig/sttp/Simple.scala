package de.wittig.sttp

import io.circe.generic.auto.*
import sttp.client3.*
import sttp.client3.circe.*

object Simple extends App:

  private val backend = HttpURLConnectionBackend()

  val response = quickRequest
    .get(uri"https://httpbin.org/get")
    .send(backend)

  println(response.code)
  println(response.body)
