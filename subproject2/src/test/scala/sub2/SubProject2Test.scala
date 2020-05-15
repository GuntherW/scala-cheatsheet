package sub2

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.funsuite.AnyFunSuite

class SubProject2Test extends AnyFunSuite with BeforeAndAfterAll with BeforeAndAfterEach {

  test("test1") {
    println("subprojekt2 sleeping for 5 seconds")
    Thread.sleep(5000)
    assert(true)
  }
}
