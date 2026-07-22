package webapp

import model.{HelloRequest, HelloResponse}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.upickle.*

object Endpoints:

  val indexEndpoint = endpoint.get
    .in("")
    .errorOut(stringBody)
    .out(statusCode)
    .out(header[String]("Content-Type"))
    .out(stringBody)

  val aboutEndpoint = endpoint.get
    .in("about")
    .errorOut(stringBody)
    .out(statusCode)
    .out(header[String]("Content-Type"))
    .out(stringBody)

  val securedEndpoint = endpoint.get
    .in("secured")
    .errorOut(stringBody)
    .out(statusCode)
    .out(header[String]("Content-Type"))
    .out(stringBody)

  val helloEndpoint = endpoint.post
    .in("api" / "hello")
    .in(jsonBody[HelloRequest])
    .out(jsonBody[HelloResponse])

  val staticEndpoint = endpoint.get
    .in("static" / paths)
    .out(statusCode)
    .out(header[String]("Content-Type"))
    .out(byteArrayBody)

  val documentedEndpoints = List(
    indexEndpoint,
    aboutEndpoint,
    securedEndpoint,
    helloEndpoint,
    staticEndpoint
  )
