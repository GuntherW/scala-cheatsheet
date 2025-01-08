package de.wittig.tapirr.examples

import cats.effect.{IO, IOApp}
import cats.syntax.all.*
import com.comcast.ip4s.{ipv4, port}
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import sttp.tapir.*
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object Http4sExample extends IOApp.Simple {

  val helloWorldEndpoint =
    endpoint
      .get
      .in("hello" / "world")
      .in(query[String]("name"))
      .out(stringBody)
      .serverLogic[IO](name =>
        for
          _   <- IO.println(s"Saying hello to $name")
          res <- IO.pure(Right(s"Hello $name"))
        yield res
      )

  val helloWorldRoutes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(helloWorldEndpoint)

  val swaggerEndpoints = SwaggerInterpreter().fromServerEndpoints[IO](List(helloWorldEndpoint), "My App", "1.0")

  val swaggerRoutes = Http4sServerInterpreter[IO]().toRoutes(swaggerEndpoints)

  val allRoutes = helloWorldRoutes <+> swaggerRoutes

  override def run: IO[Unit] =
    EmberServerBuilder.default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8083")
      .withHttpApp(Router("/" -> allRoutes).orNotFound)
      .build
      .use(_ => IO.println("Server started at http://localhost:8083") *> IO.never)
}
