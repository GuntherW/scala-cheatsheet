package de.codecentric.wittig.scala.scalacheck

import org.scalacheck.Prop.forAll
import org.scalacheck.{Gen, Properties}
import helper.Printer._

object WithGenerators extends Properties("String") {

  private val gen1: Gen[String] = Gen.alphaNumStr.map(_.toLowerCase())
  private val gen2: Gen[String] = Gen.alphaStr

  property("startsWith") = forAll(gen1, gen2) { (a: String, b: String) =>
    printn(a, b)
    (a + b).startsWith(a)
  }

  private val genConst = Gen.const("hallo")
  property("const") = forAll(genConst) { a: String =>
    println(a)
    a == "hallo"
  }
  private val genOneOf = Gen.oneOf("hallo", "hello")
  property("oneOf") = forAll(genOneOf) { a: String =>
    println(a)
    a == "hallo" || a == "hello"
  }
}
