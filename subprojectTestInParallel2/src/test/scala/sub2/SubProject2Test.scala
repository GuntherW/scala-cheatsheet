package sub2

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.funsuite.AnyFunSuite

class SubProject2Test extends AnyFunSuite with BeforeAndAfterAll with BeforeAndAfterEach {

  test("test1") {
    val sleepTime = 3000
    println(Console.BLUE + s"subprojekt2 sleeping for $sleepTime ms" + Console.RESET)
    Thread.sleep(sleepTime)
    println(Console.BLUE + s"subprojekt2 slept for $sleepTime ms" + Console.RESET)
    assert(true)
  }
}
