package de.wittig.http4s.loadbalancer

import org.http4s.*
import cats.effect.*
import cats.syntax.all.*
import com.comcast.ip4s.{host, port, Host, Port}
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import pureconfig.ConfigReader
import pureconfig.*
import pureconfig.generic.derivation.default.*

opaque type Urls = Vector[Uri]
object Urls:
  def apply(urls: Vector[Uri]): Urls = urls
  val roundRobin: Urls => Urls       = vector =>
    if (vector.isEmpty) vector
    else vector.tail :+ vector.head
  val first: Urls => Option[Uri]     = _.headOption

extension (urls: Vector[Uri])
  def toUrls: Urls = Urls(urls)

object Loadbalancer:

  def apply(
      backends: Ref[IO, Urls],
      sendAndExpect: (Request[IO], Uri) => IO[String],
      addPathToBackend: (Request[IO], Uri) => IO[Uri],
      updateFunction: Urls => Urls,
      extractor: Urls => Option[Uri],
  ): Resource[IO, HttpRoutes[IO]] =
    val dsl = Http4sDsl[IO]
    import dsl.*

    val routes = HttpRoutes.of[IO] { request =>
      backends.getAndUpdate(updateFunction).map(extractor).flatMap {
        _.fold(Ok("All backends are inactive")) { backendUri =>
          for
            uri      <- addPathToBackend(request, backendUri)
            response <- sendAndExpect(request, uri)
            result   <- Ok(response)
          yield result
        }
      }
    }
    Resource.pure(routes)

object Replica extends IOApp:

  override def run(args: List[String]): IO[ExitCode] =
    val port = args(0).toInt
    val host = "localhost"

    val dsl = Http4sDsl[IO]
    import dsl.*

    val routes = HttpRoutes.of[IO] { request =>
      Ok(s"[replica:$port] You have accessed ${request.uri.path}")
    }

    val maybeServer =
      for
        h <- Host.fromString(host)
        p <- Port.fromInt(port)
      yield EmberServerBuilder
        .default[IO]
        .withHost(h)
        .withPort(p)
        .withHttpApp(routes.orNotFound)
        .build

    maybeServer.map(_.use(_ => IO.println(s"Replica - port $port") *> IO.never))
      .getOrElse(IO.println("Host/Port combo not ok"))
      .as(ExitCode.Success)

object Server extends IOApp.Simple:

  private def getSeedNodes: IO[Urls] =
    IO.fromEither(
      ConfigSource
        .default.at("backends").load[List[String]]
        .map(string => string.map(Uri.unsafeFromString))
        .map(_.toVector)
        .leftMap(e => new RuntimeException("Can't parse application.conf" + e))
    ).map(_.toUrls)

  private def sendReq(client: Client[IO]): (Request[IO], Uri) => IO[String] = (req: Request[IO], uri: Uri) =>
    client.expect[String](req.withUri(uri))

  private def addRequestPathToBackend(req: Request[IO], uri: Uri): IO[Uri] =
    IO.pure {
      uri / req.uri.path.renderString.dropWhile(_ != '/')
    }

  override def run: IO[Unit] =
    val serverResource =
      for
        seedNodes    <- Resource.eval(getSeedNodes)
        backends     <- Resource.eval(Ref.of[IO, Urls](seedNodes))
        client       <- EmberClientBuilder.default[IO].build
        loadbalancer <- Loadbalancer(
                          backends,
                          sendReq(client),
                          addRequestPathToBackend,
                          Urls.roundRobin,
                          Urls.first
                        )
        server       <- EmberServerBuilder
                          .default[IO]
                          .withHost(host"localhost")
                          .withPort(port"7070")
                          .withHttpApp(loadbalancer.orNotFound)
                          .build
      yield server
    serverResource.use(_ => IO.println("Loadbalancer") *> IO.never)
