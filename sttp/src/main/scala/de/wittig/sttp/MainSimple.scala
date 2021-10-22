package de.wittig.sttp

import io.circe.generic.auto._
import sttp.client3._
import sttp.client3.circe._

case class GitHubResponse(total_count: Int, items: List[GitHubItem])
case class GitHubItem(name: String, stargazers_count: Int, html_url: String)

object MainSimple extends App {

  private val backend = HttpURLConnectionBackend()
  private val query   = "language:scala"
//  private val sort    = Some("stars")
  private val sort    = None

  private val request = basicRequest
    .get(uri"https://api.github.com/search/repositories?q=$query&sort=$sort")
    .response(asJson[GitHubResponse])
    .header("a", "b")

  request
    .send(backend)
    .body
    .foreach(_.items.foreach(println))

}
