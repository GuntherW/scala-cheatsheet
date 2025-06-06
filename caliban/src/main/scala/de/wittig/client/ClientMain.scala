package de.wittig.client

import caliban.*
import caliban.client.Operations.RootQuery
import de.wittig.client.generated.Client.*
import sttp.client3.*

import scala.concurrent.duration.DurationInt
import scala.util.chaining.*

case class CharacterView(name: String, nickname: List[String], origin: Origin)

object ClientMain extends App {

  private val backend = HttpClientSyncBackend()

  private val selection = Character.name ~ Character.nicknames ~ Character.origin

  private val query = Query.character("James Holden") {
    selection.mapN(CharacterView.apply)
  }

  query
    .toRequest(uri"http://localhost:8088/api/graphql")
    .readTimeout(2.seconds)
    .send(backend)
    .body
    .tap(println)
}
