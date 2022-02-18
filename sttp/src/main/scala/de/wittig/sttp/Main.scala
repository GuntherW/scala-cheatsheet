package de.wittig.sttp
import io.circe.generic.auto.*
import sttp.client3.*
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import sttp.client3.circe.*

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

object Main extends App:

  private val backend = AsyncHttpClientFutureBackend()
  private val query   = "language:scala"
  private val sort    = Some("stars")

  private val request = basicRequest
    .get(uri"https://api.github.com/search/repositories?q=$query&sort=$sort")
    .response(asJson[GitHubResponse])

  val res = request.send(backend).map { response =>
    response.body match
      case Left(error) => println(s"Error when executing request: $error")
      case Right(data) =>
        println(s"Found ${data.total_count} Scala projects.")
        println(s"Showing ${data.items.size} with most stars:")
        data.items.foreach { item =>
          println(s"  ${item.name} (${item.stargazers_count}) ${item.html_url}")
        }
  }

  Await.result(res, 3.seconds)
  backend.close()
