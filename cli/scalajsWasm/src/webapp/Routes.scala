package webapp

import cats.Id
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object Routes:
  private val docsEndpoints = SwaggerInterpreter().fromEndpoints[Id](
    Endpoints.documentedEndpoints,
    "scalajsWasm API",
    "1.0.0"
  )

  val allServerEndpoints: List[ServerEndpoint[Any, Id]] =
    PageRoutes.serverEndpoints ++ ApiRoutes.serverEndpoints ++ docsEndpoints
