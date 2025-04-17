package de.wittig.sttp

import sttp.client4.*

import java.nio.file.Files
import java.nio.file.Path

object UploadFile extends App:
  withTemporaryFile("Hello, World!".getBytes) { file =>
    val request = basicRequest
      .body(file)
      .post(uri"https://httpbin.org/post")

    val backend: SyncBackend                       = DefaultSyncBackend()
    val response: Response[Either[String, String]] = request.send(backend)

    // the uploaded data should be echoed in the "data" field of the response body
    println(response.body)
  }

  private def withTemporaryFile[T](data: Array[Byte])(f: Path => T): T = {
    val file = Files.createTempFile("sttp", "demo")
    try
      Files.write(file, data)
      f(file)
    finally
      val _ = Files.deleteIfExists(file)
  }
