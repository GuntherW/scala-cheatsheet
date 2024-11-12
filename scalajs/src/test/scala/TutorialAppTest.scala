import org.scalajs.dom.document
import utest.*

object TutorialAppTest extends TestSuite:

  // Initialize App
  TutorialApp.appendPar(document.body, "Hallo Welt")

  def tests: Tests = Tests {
    test("Hallo Welt") {
      assert(document.querySelectorAll("p").count(_.textContent == "Hallo Welt") == 1)
    }
  }
