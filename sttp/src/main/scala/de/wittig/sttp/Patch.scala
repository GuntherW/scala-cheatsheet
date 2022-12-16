package de.wittig.sttp

import io.circe.generic.auto.*
import sttp.client3.*
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import sttp.client3.circe.*
import sttp.model.StatusCode

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

object Patch extends App:

//  private val backend = HttpURLConnectionBackend() // does not support method PATCH
  private val backend = HttpClientSyncBackend()
  private val request = basicRequest
    .get(uri"http://httpbin.org/status/200")

  val req1  = request
  val req2a = request.response(asString)
  val req2b = request.response(asStringAlways)
  val req3  = request.response(asString.getRight)

  val response = request
    .send(backend)

  req2b.send(backend)

  response
    .body
    .foreach(println)

case object Dog
case object Cat
case object Monkey
