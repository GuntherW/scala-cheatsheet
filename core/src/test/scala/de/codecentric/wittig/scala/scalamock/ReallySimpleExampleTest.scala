package de.codecentric.wittig.scala.scalamock

import de.codecentric.wittig.scala.scalamock.Greetings.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalamock.stubs.Stubs

class ReallySimpleExampleTest extends AnyFunSuite, Matchers, Stubs:
  
  test("sayHello"):
    val formatterStub = stub[Formatter]

    formatterStub.format.returns(_ => "Ah, Mr Bond. I've been expecting you")

    Greetings.sayHello("Mr Bond", formatterStub) shouldBe "Ah, Mr Bond. I've been expecting you"

    formatterStub.format.times shouldBe 1 // method called exactly once
    formatterStub.format.calls shouldBe List("Mr Bond") // check that name is Mr Bond, this list has one item per each method invokation
