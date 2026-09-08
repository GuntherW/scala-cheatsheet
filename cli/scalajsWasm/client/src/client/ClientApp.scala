package client

import model.{HelloRequest, HelloResponse}
import org.scalajs.dom
import scala.scalajs.js
import upickle.default.{read, write}

object ClientApp:
  def main(args: Array[String]): Unit =
    val button = Option(dom.document.getElementById("hello-btn"))
      .getOrElse(throw new IllegalStateException("Element 'hello-btn' nicht gefunden"))
    val nameInput = dom.document.getElementById("name") match
      case el: dom.HTMLInputElement => el
      case _                        => throw new IllegalStateException("Element 'name' ist kein Input-Feld")
    val result = Option(dom.document.getElementById("result"))
      .getOrElse(throw new IllegalStateException("Element 'result' nicht gefunden"))

    button.addEventListener(
      "click",
      (_: dom.Event) =>
        val requestBody = write(HelloRequest(nameInput.value))
        dom.fetch(
          "/api/hello",
          new dom.RequestInit {
            method = dom.HttpMethod.POST
            // DOM-Interop erfordert hier einen Cast, da js.Dictionary kein HeadersInit-Subtyp ist.
            headers = js.Dictionary("Content-Type" -> "application/json").asInstanceOf[dom.HeadersInit]
            body = requestBody
          }
        )
          .`then`[String](response => response.text())
          .`then`[Unit] { text =>
            val helloResponse = read[HelloResponse](text)
            result.textContent = helloResponse.message
          }
    )
