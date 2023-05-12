//> using jvm "19"
//> using dep "dev.zio::zio-http:3.0.0-RC1"
//> using scala "3.2.2"

import zio.*
import zio.http.*

object MyApp extends ZIOAppDefault:

  val app: App[Any] =
    Http.collect[Request] {
      case Method.GET -> !! / "hello" => Response.text("Hello world!")
    }

  override val run =
    Server.serve(app).provide(Server.default)
