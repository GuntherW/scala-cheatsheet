package de.wittig.sttp

import sttp.client4.*
import sttp.client4.ws.SyncWebSocket
import sttp.client4.ws.sync.*

@main
def webSocket(): Unit =
  def useWebSocket(ws: SyncWebSocket): Unit =
    ws.sendText("Hello,")
    ws.sendText("world!")

    println(ws.receiveText())
    println(ws.receiveText())

  val backend = DefaultSyncBackend()
  try
    basicRequest
      .get(uri"wss://ws.postman-echo.com/raw")
      .response(asWebSocket(useWebSocket))
      .send(backend)
  finally
    backend.close()
