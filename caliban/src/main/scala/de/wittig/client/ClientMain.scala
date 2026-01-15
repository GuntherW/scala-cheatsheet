package de.wittig.client

import caliban.*
import caliban.client.Operations.RootQuery
import de.wittig.client.generated.Client.*
import sttp.client4.*

import scala.concurrent.duration.DurationInt

case class CharacterView(name: String, nickname: List[String], origin: Origin)

@main
def clientMain(): Unit =

  val backend = DefaultSyncBackend()

  val selection = Character.name ~ Character.nicknames ~ Character.origin

  val query = Query.character("James Holden") {
    selection.mapN(CharacterView.apply)
  }

  query
    .toRequest(uri"http://localhost:8088/api/graphql")
    .readTimeout(2.seconds)
    .send(backend)
    .body
