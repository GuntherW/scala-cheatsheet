package sub1

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class SubProject1Test extends AnyFunSuite with BeforeAndAfterAll with BeforeAndAfterEach {

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    Thread.sleep(2000)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    Thread.sleep(500)
  }

  test("test1") {
    val sum = List.tabulate(100000)(_ * 3).sum
    assert(sum > 0)
  }

  test("test2") {
    assert(true)
  }

  test("test3") {
    println("subprojekt1 sleeping for 5 seconds")
    Thread.sleep(5000)
    assert(true)
  }
}
