import org.scalatest.funsuite.AnyFunSuite
import scala.language.adhocExtensions

/** Just for showing, how to run Integrationtests. (configuration done in sbt.built). Run it with
  * {{{
  *   sbt it:test
  * }}}
  * @author
  *   gunther
  */
class UnimportantIntegrationTest extends AnyFunSuite {
  test("This is a integration test") {
    assert(true)
  }
}
