package client

import model.{HelloRequest, HelloResponse}
import org.scalajs.dom
import scala.scalajs.js
import upickle.default.{read, write}

object ClientApp:
  def main(args: Array[String]): Unit =
    val button = dom.document.getElementById("hello-btn")
    val nameInput = dom.document.getElementById("name").asInstanceOf[dom.HTMLInputElement]
    val result = dom.document.getElementById("result")

    button.addEventListener(
      "click",
      (_: dom.Event) =>
        val requestBody = write(HelloRequest(nameInput.value))
        dom.fetch(
          "/api/hello",
          new dom.RequestInit {
            method = dom.HttpMethod.POST
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
