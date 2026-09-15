//> using jvm "21"
//> using dep "dev.zio::zio-http:3.11.6"

import zio.*
import zio.http.*

object MyApp extends ZIOAppDefault:

  val routes: Routes[Any, Response] =
    Routes(
      Method.GET / "hello" -> handler(Response.text("Hello world!"))
    )

  override val run: ZIO[Any, Throwable, Nothing] =
    Server.serve(routes).provide(Server.default)
