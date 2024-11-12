package de.wittig.tapirr

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

import de.wittig.tapirr.TapiNettyFutureServer2.Kitten
import io.circe.derivation.{Configuration, ConfiguredCodec}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.netty.{NettyFutureServer, NettyFutureServerBinding}

object TapiNettyFutureServer2 extends App {

  given Configuration = Configuration.default.withSnakeCaseMemberNames
  case class Kitten(id: Long, name: String, gender: String, ageInDays: Int) derives ConfiguredCodec
  case class ErrorResponse(message: String) derives ConfiguredCodec
  case class AuthenticationToken(value: String) derives ConfiguredCodec
  case class AuthenticationError(code: Int) derives ConfiguredCodec
  type User = String

  val get = endpoint
    .get
    .in("kitten")
    .errorOut(stringBody)
    .out(jsonBody[List[Kitten]])

  val secured = endpoint
    .securityIn(auth.bearer[String]().mapTo[AuthenticationToken])
    .errorOut(plainBody[Int].mapTo[AuthenticationError])
    .serverSecurityLogic(authenticate)

  def authenticate(token: AuthenticationToken): Future[Either[AuthenticationError, User]] = Future.successful(
    token.value match
      case "papa"     => Right("Papa Schlumpf")
      case "gargamel" => Right("Gargamel")
      case _          => Left(AuthenticationError(1001))
  )

  val getSecured = secured
    .get
    .in("kittenSecured")
    .out(jsonBody[List[Kitten]])

  val post: Endpoint[Unit, Kitten, (StatusCode, ErrorResponse), (StatusCode, Kitten), Any] = endpoint
    .post
    .in("kitten")
    .in(jsonBody[Kitten])
    .errorOut(statusCode)
    .errorOut(jsonBody[ErrorResponse])
    .out(statusCode)
    .out(jsonBody[Kitten])

  val delete: Endpoint[Unit, Long, (StatusCode, ErrorResponse), (StatusCode, Kitten), Any] = endpoint
    .delete
    .in("kitten")
    .in(path[Long]("id"))
    .errorOut(statusCode)
    .errorOut(jsonBody[ErrorResponse])
    .out(statusCode)
    .out(jsonBody[Kitten])

  val getEndpoint = get
    .serverLogic(_ => Future.successful[Either[String, List[Kitten]]](Right(Database.kittens)))

  val getEndpointSecured = getSecured
    .serverLogic {
      user => _ => Future.successful[Either[AuthenticationError, List[Kitten]]](Right(Database.kittens))
    }

  val postEndpoint = post
    .serverLogic { kitten =>
      Future.successful[Either[(StatusCode, ErrorResponse), (StatusCode, Kitten)]] {
        Database.kittens = Database.kittens :+ kitten
        Right((StatusCode.Ok, kitten))
      }
    }

  val deleteEndpoint = delete
    .serverLogic { id =>
      Future.successful[Either[(StatusCode, ErrorResponse), (StatusCode, Kitten)]] {
        Database.kittens.find(_.id == id) match
          case Some(value) =>
            Database.kittens = Database.kittens.filter(_.id != id)
            Right((StatusCode.NoContent, value))
          case None        => Left((StatusCode.NotFound, ErrorResponse("Kitten not found")))
      }
    }

  val endpoints = List(getEndpoint, getEndpointSecured, postEndpoint, deleteEndpoint)

  val serverBinding: NettyFutureServerBinding = Await.result(
    NettyFutureServer()
      .port(9091)
      .host("localhost")
      .addEndpoints(endpoints)
      .start(),
    Duration.Inf
  )
}

object Database:
  var kittens: List[Kitten] = List(
    Kitten(1L, "mew", "male", 20),
    Kitten(2L, "mews", "female", 25),
    Kitten(3L, "smews", "female", 29)
  )
