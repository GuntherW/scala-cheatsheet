package de.wittig

import zio.*
import zio.http.*
import zio.http.Method.GET

object GreetingServer extends ZIOAppDefault {
  private val routes =
    Routes(
      GET / Root                            -> handler(Response.text("Greetings at your service")),
      GET / "greet"                         -> handler { (req: Request) =>
        val name = req.queryParamOrElse("name", "World")
        Response.text(s"Hello $name!")
      },
      Method.GET / "greet" / string("name") -> handler { (name: String, req: Request) =>
        Response.text(s"Hello $name")
      }
    )

  def run = Server.serve(routes).provide(Server.default)
}
