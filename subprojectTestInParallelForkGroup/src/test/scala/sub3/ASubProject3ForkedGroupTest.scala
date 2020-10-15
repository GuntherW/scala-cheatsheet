package sub2

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.funsuite.AnyFunSuite

class ASubProject3ForkedGroupTest extends AnyFunSuite with BeforeAndAfterAll with BeforeAndAfterEach {

  test("testForkedGroup A") {
    val sleepTime = 3000
    println(Console.BLUE + s"testForkedGroup A sleeping for $sleepTime ms" + Console.RESET)
    Thread.sleep(sleepTime)
    println(Console.BLUE + s"testForkedGroup A slept for $sleepTime ms" + Console.RESET)
    assert(true)
  }
}
