import zhttp.http.*
import zio.*
import zio.stream.ZStream
import zhttp.service.*

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
    case req @ Method.POST -> !! / "zio" => req.bodyAsString.map(Response.text)
  }

  private val app = collect2 ++ collect ++ collectZIO

  override def run = Server.start(8090, app)
