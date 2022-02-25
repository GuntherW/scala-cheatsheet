import zhttp.http.*
import zhttp.service.Server
import zio.stream.ZStream
import zio.*

object HelloWorld extends zio.ZIOAppDefault {

  private val collect = Http.collect[Request] {
    case Method.GET -> !!                  => Response.text(s"Hallo Welt")
    case Method.GET -> !! / "greet" / name => Response.text(s"Hallo $name!")
  }

  private val collect2 = Http.collect[Request] {
    case Method.GET -> !! / "n" => Response.text(s"Hello n!")
  }

  private val collectM = Http.collectZIO[Request] {
    case Method.GET -> !! / "m" => ZIO.succeed(Response.text("zio m"))
  }

  private val app = collect2 ++ collect ++ collectM

  override def run: ZIO[Any, Throwable, Nothing] = Server.start(8090, app)
}
