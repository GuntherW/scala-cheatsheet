import zhttp.http.*
import zhttp.service.Server
import zio.stream.ZStream
import zio.{ExitCode, URIO, ZIO, App}

object HelloWorld extends App {

  private val collect = Http.collect[Request] {
    case Method.GET -> !!                  => Response.text(s"Hallo Welt")
    case Method.GET -> !! / "greet" / name => Response.text(s"Hello $name!")
    case Method.GET -> !! / "fruits" / "a" => Response.text("Apple")
    case Method.GET -> !! / "fruits" / "b" => Response.text("Banana")
  }

  private val collect2 = Http.collect[Request] {
    case Method.GET -> !! / "n" => Response.text(s"Hello n!")
  }

  private val collectM = Http.collectZIO[Request] {
    case Method.GET -> !! / "m" => ZIO.succeed(Response.text("zio m"))
  }

  private val app = collect2 <> collect <> collectM

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    Server.start(8090, app).exitCode
}
