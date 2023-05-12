package de.wittig.sttp

import io.circe.generic.auto.*
import sttp.client3.*
import sttp.client3.circe.*

case class GitHubResponse(total_count: Int, items: List[GitHubItem])
case class GitHubItem(name: String, stargazers_count: Int, html_url: String)

object GithubSimple extends App:

  private val backend = HttpURLConnectionBackend()
  private val query   = "language:scala"
//  private val sort    = Some("stars")
  private val sort    = None

  private val request = basicRequest
    .get(uri"https://api.github.com/search/repositories?q=$query&sort=$sort")
    .response(asJson[GitHubResponse])

  request
    .send(backend)
    .body
    .foreach(_.items.foreach(println))
