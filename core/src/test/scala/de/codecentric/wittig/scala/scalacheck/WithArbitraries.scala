package de.codecentric.wittig.scala.scalacheck

import de.codecentric.wittig.scala.scalacheck.helper.Printer._
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen, Properties}

object WithArbitraries extends Properties("String") {
  private val gen: Gen[String]                = Gen.alphaUpperStr
  implicit private val arb: Arbitrary[String] = Arbitrary(gen)

  property("startsWith") = forAll { (a: String, b: String) =>
    printn(a, b)
    (a + b).startsWith(a)
  }
}
