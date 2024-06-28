package de.wittig.tapirr

import io.circe.generic.auto.*
import io.circe.{Decoder, Encoder}
import sttp.client3.Identity
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.netty.sync.NettySyncServer
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object TapiNettySyncWithErrorsServer extends App {

  case class Result(v: Int)
  case class Error(description: String)

  val maybeErrorEndpoint =
    endpoint
      .get
      .in("test")
      .in(query[Int]("input"))
      .out(jsonBody[Result])
      .errorOut(jsonBody[Error])
      .handle { input =>

        if input % 3 == 0
        then throw new RuntimeException("Multiples of 3 are unexpected")

        if input % 2 == 0
        then
          Right(Result(input))
        else
          Left(Error("That is an odd number."))
      }

  val swaggerEndpoints = SwaggerInterpreter()
    .fromServerEndpoints[Identity](List(maybeErrorEndpoint), "Meine App", "1.0")

  NettySyncServer().port(8080)
    .addEndpoint(maybeErrorEndpoint)
    .addEndpoints(swaggerEndpoints)
    .startAndWait()

}
