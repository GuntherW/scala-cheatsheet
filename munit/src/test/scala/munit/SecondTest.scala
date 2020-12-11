package munit

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Properties

//@munit.IgnoreSuite
class SecondTest extends munit.FunSuite {
  final case class Person(name: String, color: String)

  test("Second 1") {
    assume(Properties.isWin, "this test runs only on Linux") // ignore test, if running on windoof
    assert(true)
  }

  test("Secons 2") {
    val f1 = Future(1)
    f1.map(f => assert(f == 1))
  }
}
