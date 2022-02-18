import utest.*

import org.scalajs.dom.document
import org.scalajs.dom.ext.*

object TutorialAppTest extends TestSuite:

  // Initialize App
  TutorialApp.appendPar(document.body, "Hallo Welt")

  def tests: Tests = Tests {
    test("Hallo Welt") {
      assert(document.querySelectorAll("p").count(_.textContent == "Hallo Welt") == 1)
    }
  }
