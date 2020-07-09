package sub1

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class SubProject1Test extends AnyFunSuite with BeforeAndAfterAll with BeforeAndAfterEach {

  test("test3") {
    val sleepTime = 3000
    println(Console.BLUE + s"subprojekt1 sleeping for $sleepTime ms" + Console.RESET)
    Thread.sleep(sleepTime)
    println(Console.BLUE + s"subprojekt1 slept for $sleepTime ms" + Console.RESET)
    assert(true)
  }
}
