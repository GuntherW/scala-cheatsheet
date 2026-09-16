package de.wittig.sttp

import sttp.client4.*

import de.wittig.sttp.TempFiles.withTemporaryFile

@main
def uploadFile(): Unit =
  withTemporaryFile("Hello, World!".getBytes) { file =>
    val request = basicRequest
      .body(file)
      .post(uri"https://httpbin.org/post")

    val backend: SyncBackend                       = DefaultSyncBackend()
    val response: Response[Either[String, String]] = request.send(backend)

    // the uploaded data should be echoed in the "data" field of the response body
    println(response.body)
  }
