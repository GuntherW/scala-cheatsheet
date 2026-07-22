package webapp

import cats.Id
import model.HelloResponse
import sttp.tapir.server.ServerEndpoint

object ApiRoutes:

  private val helloRoute = Endpoints.helloEndpoint.serverLogicSuccess[Id](request => HelloResponse(s"Hallo, ${request.name}!"))

  val serverEndpoints: List[ServerEndpoint[Any, Id]] = List(helloRoute)
