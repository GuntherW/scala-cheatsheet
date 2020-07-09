package de.wittig.sttp
import sttp.client._
import sttp.client.circe._
import io.circe.generic.auto._
import sttp.client.asynchttpclient.future.AsyncHttpClientFutureBackend

import scala.concurrent.ExecutionContext.Implicits.global

// Case classes corresponding to the json returned by GitHub (just the
// fields that interest us).
case class GitHubResponse(total_count: Int, items: List[GitHubItem])
case class GitHubItem(name: String, stargazers_count: Int)

object Main extends App {
  implicit private val backend     = AsyncHttpClientFutureBackend()
  private val query                = "language:scala"
  private val sort: Option[String] = Some("stars")

  // Describing the request: specifying the method, uri and how to handle
  // the response. The `query` parameter is automatically url-encoded
  // `sort` will be unwrapped if `Some(_)`, and removed if `None`.
  private val request = basicRequest
    .get(uri"https://api.github.com/search/repositories?q=$query&sort=$sort")
    .response(asJson[GitHubResponse])

//  request.send().map { response =>
  // The body will be a `Left(_)` in case of a non-2xx response, or a json
  // deserialization error. It will be `Right(_)` otherwise.
  request.send().map { response =>
    response.body match {
      case Left(error) => println(s"Error when executing request: $error")
      case Right(data) =>
        println(s"Found ${data.total_count} Scala projects.")
        println(s"Showing ${data.items.size} with most stars:")
        data.items.foreach { item =>
          println(s"  ${item.name} (${item.stargazers_count})")
        }
    }
  }
}
