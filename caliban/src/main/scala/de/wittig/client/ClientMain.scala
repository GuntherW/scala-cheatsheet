package de.wittig.client

import caliban.*
import caliban.client.Operations.RootQuery
import caliban.client.SelectionBuilder
import caliban.schema.Schema.auto.*
import generated.Client.*
import generated.Client.Character.*
import generated.Client.Query.*
import sttp.client3.*
import scala.util.chaining.*

object ClientMain extends App {
  private val serverUrl = uri"http://localhost:8088/api/graphql"
  private val backend   = HttpClientSyncBackend()

  case class CharacterView(name: String, nickname: List[String], origin: Origin)

  val selection: SelectionBuilder[Character, (String, List[String], Origin)] =
    Character.name ~ Character.nicknames ~ Character.origin

  val query = Query.character("James Holden") {
    selection.mapN(CharacterView.apply)
  }

  query
    .toRequest(serverUrl)
    .send(backend)
    .body
    .tap(println)

}
