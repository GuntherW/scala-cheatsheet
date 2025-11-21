package de.wittig.sttp

import sttp.client4.*
import sttp.model.{HeaderNames, MediaType}

@main
def simple(): Unit =

  val backend = DefaultSyncBackend()

  val response = quickRequest
    .get(uri"https://httpbin.org/get")
    .contentType(MediaType.ApplicationJson)
    .header(HeaderNames.Accept, MediaType.ApplicationJson.toString) // Set the Accept header
    .send(backend)

  println(response.code)
  println(response.body)
