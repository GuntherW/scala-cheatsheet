package de.wittig.sttp

import sttp.client4.*

import java.nio.charset.StandardCharsets

import de.wittig.sttp.TempFiles.withTemporaryFile

@main
def uploadFile(): Unit =
  withTemporaryFile("Hello, World!".getBytes(StandardCharsets.UTF_8)) { file =>
    val request = basicRequest
      .body(file)
      .post(uri"https://httpbin.org/post")

    val backend: SyncBackend                       = DefaultSyncBackend()
    val response: Response[Either[String, String]] = request.send(backend)

    // the uploaded data should be echoed in the "data" field of the response body
    println(response.body)
  }
