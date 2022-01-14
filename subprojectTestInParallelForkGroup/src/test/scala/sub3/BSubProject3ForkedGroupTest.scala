package sub2

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.funsuite.AnyFunSuite
import scala.language.adhocExtensions

class BSubProject3ForkedGroupTest extends AnyFunSuite with BeforeAndAfterAll with BeforeAndAfterEach {

  test("testForkedGroup B") {
    val sleepTime = 3000
    println(Console.BLUE + s"testForkedGroup B sleeping for $sleepTime ms" + Console.RESET)
    Thread.sleep(sleepTime)
    println(Console.BLUE + s"testForkedGroup B slept for $sleepTime ms" + Console.RESET)
    assert(true)
  }
}
