package de.wittig.rockthejvm

import zio.*
import zio.http.*
import zio.http.HttpAppMiddleware.*
import zio.http.ChannelEvent.UserEvent.{HandshakeComplete, HandshakeTimeout}
import zio.http.ChannelEvent.{ChannelRead, ChannelUnregistered, UserEventTriggered}
import zio.http.middleware.Cors.CorsConfig
import zio.http.model.Method
import zio.http.socket.{WebSocketChannelEvent, WebSocketFrame}

object ZioHttpApp extends ZIOAppDefault:

  private val port = 9000

  // TODO: Fix Generation of CSRF token
  private val app: UHttpApp = Http.collect[Request] {
    case Method.GET -> !! / "generateCSRF" => Response.text("Generate CSRF")
  } @@ withContentSecurityPolicy("a")

  // TODO: Fix usage of CSRF token
  private val zApp: UHttpApp = Http.collectZIO[Request] {
    case Method.POST -> !! / "validateCSRF" => Random
        .nextIntBetween(3, 5)
        .map(n => Response.text(s"Validate CSRF Random:[$n]"))
  } @@ withContentSecurityPolicy("b")

  private val authApp: UHttpApp = Http.collect[Request] {
    case Method.GET -> !! / "secret" / "owls" => Response.text("secret")
  } @@ basicAuth("gunther", "cc")

  // websockets
  private val sarcastic: String => String = txt =>
    txt.toList.zipWithIndex.map {
      case (c, i) => if (i % 2 == 0) c.toUpper else c.toLower
    }.mkString

  private val wsLogic: Http[Any, Throwable, WebSocketChannelEvent, Unit] = Http.collectZIO[WebSocketChannelEvent] {
    case ChannelEvent(channel, ChannelRead(WebSocketFrame.Text(message))) => channel.writeAndFlush(WebSocketFrame.text(sarcastic(message)))
    case ChannelEvent(channel, UserEventTriggered(event))                 =>
      event match
        case HandshakeComplete => Console.printLine("Websocket started")
        case HandshakeTimeout  => Console.printLine("Connection failed")
    case ChannelEvent(channel, ChannelUnregistered)                       => Console.printLine("Connection closed")
  }

  private val wsApp = Http.collectZIO[Request] {
    case Method.GET -> !! / "ws" => wsLogic.toSocketApp.toResponse
  }

  // Reihenfolge ist wichtig, da die Middleware "weitervererbt" wird
  private val combined = app ++ wsApp ++ authApp ++ zApp

  // CORS (Erlauben von Aufruf anderer Domains)
  private val corsConfig = CorsConfig(
    anyOrigin = false,
    anyMethod = false,
    allowedOrigins = s => s.equals("localhost"),
    allowedMethods = Some(Set(Method.GET, Method.POST)),
  )

  private val httpWithMiddleware = combined @@ cors(corsConfig)

  private val program = for {
    _ <- Console.printLine(s"Starting server at http://localhost:$port")
    _ <- Server.serve(httpWithMiddleware)
  } yield ()

  private val configLayer = ServerConfig.live(ServerConfig.default.port(9000))
  override def run        = program.provide(configLayer, Server.live)
