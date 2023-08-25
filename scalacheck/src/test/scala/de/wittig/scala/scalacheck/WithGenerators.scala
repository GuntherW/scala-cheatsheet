package de.wittig.scala.scalacheck

import de.wittig.scala.scalacheck.helper.Printer.*
import org.scalacheck.Prop.forAll
import org.scalacheck.{Gen, Properties}
import scala.language.adhocExtensions

object WithGenerators extends Properties("String"):

  private val gen1: Gen[String] = Gen.alphaNumStr.map(_.toLowerCase())
  private val gen2: Gen[String] = Gen.alphaStr
  private val gen3: Gen[String] = Gen.choose(5, 15).flatMap(Gen.stringOfN(_, Gen.alphaNumChar))

  private val zeroOrMoreDigits = Gen.someOf(1 to 9)
  private val oneOrMoreDigits  = Gen.atLeastOne(1 to 9)

  private val fiveDice             = Gen.pick(5, 1 to 6)
  private val threeLetters         = Gen.pick(3, 'A' to 'Z')
  private val threeLettersFromGens = Gen.pick(2, gen1, gen2, gen3)

  property("startsWith") = forAll(gen1, gen2, gen3) { (a: String, b: String, c: String) =>
    printn(a, b, c)
    (a + b).startsWith(a)
  }

  private val genConst = Gen.const("hallo")
  property("const") = forAll(genConst) { (a: String) =>
    a == "hallo"
  }

  private val genOneOf = Gen.oneOf("hallo", "hello")
  property("oneOf") = forAll(genOneOf) { (a: String) =>
    a == "hallo" || a == "hello"
  }
