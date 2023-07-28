package de.wittig.sttp

import sttp.client4.*

object Simple extends App:

  private val backend = DefaultSyncBackend()

  val response = quickRequest
    .get(uri"https://httpbin.org/get")
    .send(backend)

  println(response.code)
  println(response.body)
