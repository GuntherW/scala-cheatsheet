package de.wittig.sttp

import sttp.client3._
import sttp.client3.akkahttp.AkkaHttpBackend
import sttp.ws.WebSocket

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object WebSocketAsync extends App {

  private val backend = AkkaHttpBackend()

  def useWebSocket(ws: WebSocket[Future]): Future[Unit] = {
    def send(i: Int) = ws.sendText(s"Hello $i!")
    def receive()    = ws.receiveText().map(t => println(s"Received: $t"))
    for {
      _ <- send(1)
      _ <- send(2)
      _ <- receive()
      _ <- receive()
    } yield ()
  }

  basicRequest
    .response(asWebSocket(useWebSocket))
    .get(uri"wss://echo.websocket.org")
    .send(backend)
    .onComplete(_ => backend.close())
}
