package de.wittig.zio

import zio.*
import zio.test.*

case class Person(name: String, age: Int):
  def spellName: String         = name.toUpperCase
  def saySomething: UIO[String] = ZIO.succeed(s"Hi, I'm $name")

object MyTestSec extends ZIOSpecDefault:
  def spec = test("First Test") {
    val person = Person("Gunther", 123)

    assert(person.spellName)(Assertion.equalTo("GUNTHER"))
    assertTrue(person.spellName == "GUNTHER")
  }

object MyFirstEffectSpec extends ZIOSpecDefault:
  def spec = test("First Effect Test") {
    val person = Person("Gunther", 123)

    assertZIO(person.saySomething)(Assertion.equalTo("Hi, I'm Gunther"))
    assertZIO(person.saySomething)(Assertion.assertion("should be a greeting")(gr => gr == "Hi, I'm Gunther"))
  }

object ASuiteSpec extends ZIOSpecDefault:
  def spec = suite("suite of tests")(
    test("first test") {
      assertTrue(1 + 1 == 2)
    },
    test("second test") {
      assertZIO(ZIO.succeed("Scala"))(Assertion.hasSizeString(Assertion.equalTo(5)) && Assertion.startsWithString("S"))
    },
    suite("nested suite")(
      test("first nested test") {
        assert(List(1, 2, 3))(Assertion.isNonEmpty && Assertion.hasSameElements(List(1, 2, 3)))
      },
      test("second nested test") {
        assert(List(1, 2, 3).headOption)(Assertion.equalTo(Some(1)))
      }
    )
  )
