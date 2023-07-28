package de.wittig.sttp

import io.circe.generic.auto.*
import sttp.client4.*
import sttp.client4.circe.*

case class GitHubResponse(total_count: Int, items: List[GitHubItem])
case class GitHubItem(name: String, stargazers_count: Int, html_url: String)

object GithubSimple extends App:

  private val backend = DefaultSyncBackend()
  private val query   = "language:scala"
  private val sort    = Some("stars")
//  private val sort    = None

  private val request = basicRequest
    .get(uri"https://api.github.com/search/repositories?q=$query&sort=$sort")
    .response(asJson[GitHubResponse])

  request
    .send(backend)
    .body
    .foreach(_.items.foreach(println))
