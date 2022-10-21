package de.wittig.rockthejvm

import zio.*
import zhttp.http.*
import zhttp.http.middleware.Cors.CorsConfig
import zhttp.service.ChannelEvent.*
import zhttp.service.{ChannelEvent, Server}
import zhttp.service.ChannelEvent.UserEvent.*
import zhttp.socket.{WebSocketChannelEvent, WebSocketFrame}

object ZIOHTTP extends ZIOAppDefault:

  val port = 9000

  val app: UHttpApp = Http.collect[Request] {
    case Method.GET -> !! / "owls" => Response.text("Hello, owls!")
  } @@ Middleware.csrfGenerate()

  val zApp: UHttpApp = Http.collectZIO[Request] {
    case Method.POST -> !! / "owls" => Random
        .nextIntBetween(3, 5)
        .map(n => Response.text(s"Hello $n owls"))
  } @@ Middleware.csrfValidate()

  val authApp: UHttpApp = Http.collect[Request] {
    case Method.GET -> !! / "secret" / "owls" => Response.text("secret")
  } @@ Middleware.basicAuth("gunther", "cc")

  // CORS (Erlauben von Aufruf anderer Domains)
  val corsConfig = CorsConfig(
    anyOrigin = false,
    anyMethod = false,
    allowedOrigins = s => s.equals("localhost"),
    allowedMethods = Some(Set(Method.GET, Method.POST)),
  )

  // websockets
  val sarcastic: String => String = txt =>
    txt.toList.zipWithIndex.map {
      case (c, i) => if (i % 2 == 0) c.toUpper else c.toLower
    }.mkString

  val wsLogic: Http[Any, Throwable, WebSocketChannelEvent, Unit] = Http.collectZIO[WebSocketChannelEvent] {
    case ChannelEvent(channel, ChannelRead(WebSocketFrame.Text(message))) => channel.writeAndFlush(WebSocketFrame.text(sarcastic(message)))
    case ChannelEvent(channel, UserEventTriggered(event))                 =>
      event match
        case HandshakeComplete => Console.printLine("Websocket started")
        case HandshakeTimeout  => Console.printLine("Connection failed")
    case ChannelEvent(channel, ChannelUnregistered)                       => Console.printLine("Connection closed")
  }

  val wsApp = Http.collectZIO[Request] {
    case Method.GET -> !! / "ws" => wsLogic.toSocketApp.toResponse
  }

  // Reihenfolge ist wichtig, da die Middleware "weitervererbt" wird
  val combined = app ++ wsApp ++ authApp ++ zApp

  val httpWithMiddleware = combined @@ Middleware.debug @@ Middleware.cors(corsConfig) @@ Verbose.log
  val program            = for {
    _ <- Console.printLine(s"Starting server at http://localhost:$port")
    _ <- Server.start(port, httpWithMiddleware)
  } yield ()

  override def run = program

// Middleware
object Verbose {
  def log[R, E >: Exception]: Middleware[R, E, Request, Response, Request, Response] = new Middleware[R, E, Request, Response, Request, Response] {
    override def apply[R1 <: R, E1 >: E](http: Http[R1, E1, Request, Response]): Http[R1, E1, Request, Response] =
      http
        .contramapZIO[R1, E1, Request] { request => // Middleware, auf dem Request mit contramap
          for {
            _ <- Console.printLine(s"< [request] ${request.method} ${request.path} ${request.version}")
            _ <- ZIO.foreach(request.headers.toList)(header => Console.printLine(s"< [header] ${header._1}: ${header._2}"))
          } yield request
        }
        .mapZIO[R1, E1, Response](response => // Middleware, auf der Response mit map
          for {
            _ <- Console.printLine(s"> [status] ${response.status}")
            _ <- ZIO.foreach(response.headers.toList)(header => Console.printLine(s"> [header] ${header._1}: ${header._2}"))
            _ <- Console.printLine("-" * 50)
          } yield response
        )
  }
}
