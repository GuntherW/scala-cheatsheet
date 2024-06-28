package de.wittig.http4s

import cats.effect.*
import cats.effect.std.Random
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.server.*
import org.http4s.implicits.*
import org.http4s.ember.server.*
import com.comcast.ip4s.*
import io.circe.Encoder
import io.circe.syntax.*
import io.circe.Encoder

object SimpleRoutes extends IOApp:

  def okOderFehler2(fa: IO[String]): IO[Response[IO]] =
    fa
      .flatMap(a => Ok(a))
      .handleErrorWith {
        case t: IllegalStateException    => BadRequest("state    " + t.getMessage)
        case t: IllegalArgumentException => BadRequest("argument " + t.getMessage)
      }

  val ok =
    for
      _ <- IO.println("a start")
      a <- IO("a")
      _ <- IO.println("a end")
    yield a

  val error =
    for
      _ <- IO.println("a start")
      a <- IO("a")
      _ <- IO.raiseError(new IllegalStateException("Boom"))
      _ <- IO.println("a end")
    yield a

  val routes: HttpRoutes[IO] = HttpRoutes.of {
    case GET -> Root / "welcome" / user => Ok(s"Welcome, $user")
    case GET -> Root / "ok"             => okOderFehler2(ok)
    case GET -> Root / "fehler"         => okOderFehler2(error)
  }

  val server = EmberServerBuilder
    .default[IO]
    .withHost(ipv4"0.0.0.0")
    .withPort(port"9080")
    .withHttpApp(routes.orNotFound)
    .build

  override def run(args: List[String]): IO[ExitCode] =
    server
      .use(_ => IO.never)
      .as(ExitCode.Success)
