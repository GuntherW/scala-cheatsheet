//> using jvm "21"
//> using dep "dev.zio::zio-http:3.0.1"

import zio.*
import zio.http.*

object MyApp extends ZIOAppDefault:

  val app: App[Any] =
    Http.collect[Request] { case Method.GET -> !! / "hello" =>
      Response.text("Hello world!")
    }

  override val run =
    Server.serve(app).provide(Server.default)
