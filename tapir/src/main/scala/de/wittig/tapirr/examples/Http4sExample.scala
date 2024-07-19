package de.wittig.tapirr.examples

import cats.effect.{IO, IOApp}
import cats.syntax.all.*
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
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
    BlazeServerBuilder[IO]
      .bindHttp(8083, "localhost")
      .withHttpApp(Router("/" -> allRoutes).orNotFound)
      .resource
      .use(_ => IO.never)
}
