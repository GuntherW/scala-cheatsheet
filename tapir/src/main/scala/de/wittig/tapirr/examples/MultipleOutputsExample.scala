package de.wittig.tapirr.examples

import scala.util.hashing.MurmurHash3

import sttp.client3.Identity
import sttp.model.*
import sttp.tapir.*
import sttp.tapir.server.netty.sync.NettySyncServer
import sttp.tapir.swagger.bundle.SwaggerInterpreter

enum AvatarError:
  case Unauthorized
  case NotFound
  case Other(msg: String)

enum AvatarSuccess:
  case Found(bytes: Array[Byte])
  case Redirect(location: String)

val o1: EndpointOutput[Unit]                   = statusCode(StatusCode.TemporaryRedirect)
val o2: EndpointOutput[String]                 = header[String](HeaderNames.Location)
val o3: EndpointOutput[AvatarSuccess.Redirect] = o1.and(o2).mapTo[AvatarSuccess.Redirect]

val successOutput: EndpointOutput[AvatarSuccess] =
  oneOf(
    oneOfVariant(o3),
    oneOfVariant(byteArrayBody.mapTo[AvatarSuccess.Found])
  )

val errorOutput: EndpointOutput[AvatarError] =
  oneOf(
    oneOfVariantSingletonMatcher(statusCode(StatusCode.Unauthorized))(AvatarError.Unauthorized),
    oneOfVariantSingletonMatcher(statusCode(StatusCode.NotFound))(AvatarError.NotFound),
    oneOfVariant(stringBody.mapTo[AvatarError.Other]),
  )

object MultipleOutputsExample extends App {

  case class Input(operation: String, value1: Int, value2: Int)
  case class Output(result: Result, hash: String)
  case class Result(res: Int)
  case class Error(description: String)

  def hash(result: Int) = Output(Result(result), MurmurHash3.stringHash(result.toString).toString)

  val avatarEndpoint =
    endpoint
      .get
      .in("user" / "avatar")
      .in(query[Int]("id"))
      .out(successOutput)
      .errorOut(errorOutput)

      // Int => Either[AvatarError, AvatarSuccess]
      .handle {
        case 1 => Right(AvatarSuccess.Found(";-)".getBytes))
        case 2 => Right(AvatarSuccess.Redirect("https://pbs.twimg.com/profile_images/544783695366737920/ab_zbeJX_400x400.png"))
        case 3 => Left(AvatarError.Unauthorized)
        case 4 => Left(AvatarError.NotFound)
        case 5 => Left(AvatarError.Other("We don't like this user."))
      }
  val swaggerEndpoints = SwaggerInterpreter().fromServerEndpoints[Identity](List(avatarEndpoint), "Meine App", "1.0")

  NettySyncServer().port(8082)
    .addEndpoint(avatarEndpoint)
    .addEndpoints(swaggerEndpoints)
    .startAndWait()

}
