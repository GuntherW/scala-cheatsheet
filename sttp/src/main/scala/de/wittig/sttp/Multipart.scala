package de.wittig.sttp

import sttp.client4.*
import sttp.model.MediaType

import java.nio.charset.StandardCharsets

import de.wittig.sttp.TempFiles.withTemporaryFile

@main
def multipartDemo(): Unit =

  withTemporaryFile("Hello, World!".getBytes(StandardCharsets.UTF_8)) { file1 =>
    withTemporaryFile("<img>".getBytes(StandardCharsets.UTF_8)) { file2 =>
      val request = basicRequest
        .multipartBody(
          List(
            multipart("name", "John"),
            multipartFile("bio", file1),
            multipartFile("avatar", file2).contentType(MediaType.ImagePng),
            multipart("link", "http://john.doe.com")
          )
        )
        .post(uri"https://httpbin.org/post")

      val backend: SyncBackend                       = DefaultSyncBackend()
      val response: Response[Either[String, String]] = request.send(backend)

      // the response body should contain a "files" and "form" fields with the uploaded multipart data
      println(response.body)
    }
  }
