package de.codecentric.wittig.scala.scalacheck.munit

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Properties

//@munit.IgnoreSuite
class FirstTest extends munit.FunSuite {
  final case class Person(name: String, color: String)

  test("true") {
    assume(Properties.isWin, "this test runs only on Linux") // ignore test, if running on windoof
    assert(true)
  }

  test("Actionable errors".ignore) {
    val realPerson   = Person("Hans", "red")
    val wronglPerson = Person("Hans", "black")
    assert(realPerson == wronglPerson)
  }

  test("Actionable errors2".ignore) {
    val realPerson   = Person("Hans", "red")
    val wronglPerson = Person("Hans", "black")
    assertEquals(realPerson, wronglPerson)
  }

  test("future") {
    val f1 = Future(1)
    f1.map(f => assert(f == 1))
  }
}
