package webapp

import cats.Id
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint

import java.nio.charset.StandardCharsets

object PageRoutes:
  private val indexHtml   = html.index("Mill + Scala.js").body
  private val aboutHtml   = html.about("About").body
  private val securedHtml = html.secured("Secured").body

  private val indexRoute   = Endpoints.indexEndpoint.serverLogicSuccess[Id](_ => (StatusCode.Ok, "text/html; charset=utf-8", indexHtml))
  private val aboutRoute   = Endpoints.aboutEndpoint.serverLogicSuccess[Id](_ => (StatusCode.Ok, "text/html; charset=utf-8", aboutHtml))
  private val securedRoute = Endpoints.securedEndpoint.serverLogicSuccess[Id](_ => (StatusCode.Ok, "text/html; charset=utf-8", securedHtml))

  private val staticRoute = Endpoints.staticEndpoint.serverLogicSuccess[Id] {
    segments =>
      val relative = segments.mkString("/")
      loadResourceBytes(s"webapp/$relative") match
        case Some(bytes) => (StatusCode.Ok, contentType(relative), bytes)
        case None        => (
            StatusCode.NotFound,
            "text/plain; charset=utf-8",
            s"Not found: /static/$relative".getBytes(StandardCharsets.UTF_8)
          )
  }

  private def loadResourceBytes(resourceName: String): Option[Array[Byte]] =
    Option(getClass.getClassLoader.getResourceAsStream(resourceName)).map {
      stream =>
        try stream.readAllBytes()
        finally stream.close()
    }

  private def contentType(fileName: String): String =
    if fileName.endsWith(".css") then "text/css; charset=utf-8"
    else if fileName.endsWith(".js") then "text/javascript; charset=utf-8"
    else if fileName.endsWith(".map") then "application/json; charset=utf-8"
    else if fileName.endsWith(".wasm") then "application/wasm"
    else "application/octet-stream"

  val serverEndpoints: List[ServerEndpoint[Any, Id]] =
    List(indexRoute, aboutRoute, securedRoute, staticRoute)
