package de.wittig.sttp

import sttp.client4.*
import sttp.model.HeaderNames

object BasicAuth extends App:

  val password                 = "1234"
  val backend: SyncBackend     = DefaultSyncBackend()
  val request: Request[String] = basicRequest
    .get(uri"http://httpbin.org/basic-auth/admin/$password")
    .auth
    .basic("admin", password)
    .header(HeaderNames.XForwardedFor, "http://example.com")
    .response(asStringOrFail)

  val response: Response[String] = request.send(backend)
  println(response.show())
