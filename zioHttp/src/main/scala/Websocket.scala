import zhttp.http.*
import zhttp.service.Server
import zhttp.socket.{Socket, WebSocketFrame}
import zio.*
import zio.stream.ZStream

object Websocket:

  val socket = Socket.collect[WebSocketFrame] {
    case WebSocketFrame.Text("FOO")  => ZStream.succeed(WebSocketFrame.text("BAR"))
    case WebSocketFrame.Text("BAR")  => ZStream.succeed(WebSocketFrame.text("FOO"))
    case WebSocketFrame.Ping         => ZStream.succeed(WebSocketFrame.pong)
    case WebSocketFrame.Pong         => ZStream.succeed(WebSocketFrame.ping)
    case fr @ WebSocketFrame.Text(_) => ZStream.repeat(fr).schedule(Schedule.spaced(1.second)).take(10)
  }
