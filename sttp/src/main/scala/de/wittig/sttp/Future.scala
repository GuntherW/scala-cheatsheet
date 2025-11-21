package de.wittig.sttp

import sttp.client4.*
import sttp.client4.httpclient.HttpClientFutureBackend

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

@main
def future(): Unit =

  val backend = HttpClientFutureBackend()

  val response = quickRequest
    .get(uri"https://httpbin.org/get")
    .send(backend)

  val res = response.map { a =>
    println(a.code)
    println(a.body)
  }

  Await.result(res, 5.seconds)
  backend.close()
