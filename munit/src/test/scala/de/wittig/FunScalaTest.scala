package de.wittig

import org.scalatest.funsuite.AnyFunSuite

import scala.util.Properties

class FunScalaTest extends AnyFunSuite {
  case class Person(name: String, color: String)

  test("should run on windows only") {
    assume(Properties.isLinux, "this test runs only on Linux") // ignore test, if running on windoof
    assert(true)
  }

  ignore("Actionable errors -- bad way") {
    val realPerson   = Person("Hans", "red")
    val wronglPerson = Person("Hans", "black")
    assert(realPerson == wronglPerson)
  }

}
