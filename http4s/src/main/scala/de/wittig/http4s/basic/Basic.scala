package de.wittig.http4s.basic

import cats.effect.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.server.*
import org.http4s.implicits.*
import org.http4s.ember.server.*
import com.comcast.ip4s.*
import cats.data.*
import org.http4s.headers.Authorization

object Basic extends IOApp:

  case class User(id: Long, name: String)

  val authedRoutes: AuthedRoutes[User, IO] = AuthedRoutes.of {
    case GET -> Root / "welcome" as user => Ok(s"Welcome, ${user.name}")
  }

  val authUserEither: Kleisli[IO, Request[IO], Either[String, User]] = Kleisli { req =>
    val authHeader: Option[Authorization] = req.headers.get[Authorization]
    authHeader match
      case Some(value) => value match
          case Authorization(BasicCredentials(creds)) => IO(Right(User(1, creds._1)))
          case _                                      => IO(Left("No basic credentials"))
      case None        => IO(Left("Unauthorized"))
  }
  val onFailure: AuthedRoutes[String, IO]                            = Kleisli((_: AuthedRequest[IO, String]) => OptionT.pure[IO](Response[IO](status = Status.Unauthorized)))
  val authMiddleware: AuthMiddleware[IO, User]                       = AuthMiddleware(authUserEither, onFailure)
  val serviceKleisli: HttpRoutes[IO]                                 = authMiddleware(authedRoutes)

  val server = EmberServerBuilder
    .default[IO]
    .withHost(ipv4"0.0.0.0")
    .withPort(port"9081")
    .withHttpApp(serviceKleisli.orNotFound)
    .build

  override def run(args: List[String]): IO[ExitCode] =
    server
      .use(_ => IO.never)
      .as(ExitCode.Success)
