package model

import upickle.default.{ReadWriter, macroRW}

case class HelloRequest(name: String)

object HelloRequest:
  given ReadWriter[HelloRequest] = macroRW

case class HelloResponse(message: String)

object HelloResponse:
  given ReadWriter[HelloResponse] = macroRW
