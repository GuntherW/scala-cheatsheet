import org.scalajs.dom
import org.scalajs.dom.document

import scala.scalajs.js.annotation.JSExportTopLevel

object TutorialApp:

  def main(args: Array[String]): Unit =
    appendPar(document.body, "Hallo Welt")
    println("Hallo Konsole!")

  def appendPar(targetNode: dom.Node, text: String): Unit =
    val parNode = document.createElement("p")
    parNode.textContent = text
    targetNode.appendChild(parNode)
    ()

  @JSExportTopLevel("addClickedMessage")
  def addClickedMessage(): Unit =
    appendPar(document.body, "Du hast den Knopf gedrückt")
