package de.wittig.tapirr.examples

import scala.util.hashing.MurmurHash3

import io.circe.{Decoder, Encoder}
import io.circe.generic.auto.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.netty.sync.NettySyncServer
import sttp.tapir.swagger.bundle.SwaggerInterpreter

@main
def inputMappingExample(): Unit =

  case class Input(operation: String, value1: Int, value2: Int)
  case class Output(result: Result, hash: String)
  case class Result(res: Int)
  case class Error(description: String)

  def hash(result: Int) = Output(Result(result), MurmurHash3.stringHash(result.toString).toString)

  val maybeErrorEndpoint =
    endpoint.get
      .in("operation" / path[String]("opName"))
      .in(query[Int]("value1"))
      .in(query[Int]("value2"))
      .mapInTo[Input]
      .out(jsonBody[Result])
      .out(header[String]("X-Result-Hash"))
      .mapOutTo[Output]
      .errorOut(jsonBody[Error])
      .handle { input =>
        input.operation match
          case "add" => Right(hash(input.value1 + input.value2))
          case "sub" => Right(hash(input.value1 - input.value2))
          case _     => Left(Error("Unknown operation"))
      }

  val swaggerEndpoints = SwaggerInterpreter().fromServerEndpoints(List(maybeErrorEndpoint), "Meine App", "1.0")

  NettySyncServer().port(8080)
    .addEndpoint(maybeErrorEndpoint)
    .addEndpoints(swaggerEndpoints)
    .startAndWait()
