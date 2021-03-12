package munit

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Properties

//@munit.IgnoreSuite
class FirstTest extends munit.FunSuite {
  case class Person(name: String, color: String)

  test("should run on windows only") {
    assume(Properties.isWin, "this test runs only on Linux") // ignore test, if running on windoof
    assert(true)
  }

  test("Actionable errors -- bad way".ignore) {
    val realPerson   = Person("Hans", "red")
    val wronglPerson = Person("Hans", "black")
    assert(realPerson == wronglPerson)
  }

  test("Actionable errors -- good way".ignore) {
    val realPerson   = Person("Hans", "red")
    val wronglPerson = Person("Hans", "black")
    assertEquals(realPerson, wronglPerson)
  }

  test("Futures should be ") {
    val f1 = Future(1)
    f1.map(f => assertEquals(f, 1))
  }
}
