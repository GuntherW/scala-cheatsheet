package model

import upickle.default.ReadWriter

case class HelloRequest(name: String) derives ReadWriter

case class HelloResponse(message: String) derives ReadWriter
