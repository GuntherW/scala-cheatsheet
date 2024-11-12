package de.wittig.http4s.disgest

import cats.effect.*
import com.comcast.ip4s.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.*
import org.http4s.implicits.*
import org.http4s.server.*
import org.http4s.server.middleware.authentication.DigestAuth
import org.http4s.server.middleware.authentication.DigestAuth.Md5HashedAuthStore

object Digest extends IOApp:

  case class User(id: Long, name: String)

  private val authedRoutes: AuthedRoutes[User, IO] = AuthedRoutes.of {
    case GET -> Root / "welcome" as user => Ok(s"Welcome, ${user.name}")
  }

  private val ha1: IO[String] = Md5HashedAuthStore.precomputeHash[IO]("username", "http://localhost:8080/welcome", "password")

  private val funcPass: String => IO[Option[(User, String)]] = {
    case "username" => ha1.flatMap(hash => IO(Some(User(1, "username"), hash)))
    case _          => IO(None)
  }

  private val middleware: IO[AuthMiddleware[IO, User]] = DigestAuth.applyF[IO, User]("http://localhost:8080/welcome", Md5HashedAuthStore(funcPass))
  private val digestService: IO[HttpRoutes[IO]]        = middleware.map(wrapper => wrapper(authedRoutes))

  def server(service: IO[HttpRoutes[IO]]): IO[Resource[cats.effect.IO, Server]] =
    service.map { svc =>
      EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"9082")
        .withHttpApp(svc.orNotFound)
        .build
    }

  override def run(args: List[String]): IO[ExitCode] =
    server(digestService)
      .flatMap(_.use(_ => IO.never))
      .as(ExitCode.Success)
