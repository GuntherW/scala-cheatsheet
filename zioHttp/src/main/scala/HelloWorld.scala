import zio.*
import zio.http.*
import zio.http.codec.HttpCodec.{literal, queryInt, string}
import zio.http.endpoint.*
import zio.http.model.Method
import zio.schema.{DeriveSchema, Schema}
import zio.stream.ZStream

object HelloWorld extends ZIOAppDefault:

  private val collect = Http.collect[Request] {
    case Method.GET -> !!                  => Response.text(s"Hallo /")
    case Method.GET -> !! / "greet" / name => Response.text(s"Hallo /$name")
  }

  private val collect2 = Http.collect[Request] {
    case Method.GET -> !! / "n" => Response.text(s"Hallo /n")
  }

  private val collectZIO = Http.collectZIO[Request] {
    case Method.GET -> !! / "zio"        => ZIO.succeed(Response.text("Hallo /zio"))
    case req @ Method.POST -> !! / "zio" => req.body.asString.map(Response.text).orDie
  }

  private val app = collect2 ++ collect ++ collectZIO

  override val run = Server
    .serve(app)
    .provide(
      ServerConfig.live(ServerConfig.default.port(8090)),
      Server.live
    )
