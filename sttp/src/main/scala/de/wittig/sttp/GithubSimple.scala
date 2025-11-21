package de.wittig.sttp

import io.circe.generic.auto.*
import sttp.client4.*
import sttp.client4.circe.*

case class GitHubResponse(total_count: Int, items: List[GitHubItem])
case class GitHubItem(name: String, stargazers_count: Int, html_url: String)

@main
def githubSimple(): Unit =

  val backend = DefaultSyncBackend()
  val query   = "language:scala"
  val sort    = Some("stars")
//  val sort    = None

  val request = basicRequest
    .get(uri"https://api.github.com/search/repositories?q=$query&sort=$sort")
    .response(asJson[GitHubResponse])

  request
    .send(backend)
    .body
    .foreach(_.items.foreach(println))
