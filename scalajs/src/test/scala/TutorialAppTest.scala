import utest._

import scala.scalajs.js

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.ext._

object TutorialAppTest extends TestSuite {

  // Initialize App
  TutorialApp.appendPar(document.body, "Hallo Welt")

  def tests = Tests {
    test("Hallo Welt") {
      assert(document.querySelectorAll("p").count(_.textContent == "Hallo Welt") == 1)
    }
  }
}