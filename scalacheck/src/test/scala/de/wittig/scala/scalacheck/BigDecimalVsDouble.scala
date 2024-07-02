package de.wittig.scala.scalacheck

import scala.language.adhocExtensions

import de.wittig.scala.scalacheck.helper.Printer.*
import org.scalacheck.{Arbitrary, Gen, Properties}
import org.scalacheck.Prop.forAll

object BigDecimalVsDouble extends Properties("Int"):
  private val gen: Gen[String]    = Gen.alphaUpperStr
  private given Arbitrary[String] = Arbitrary(gen)

  property("big dec") = forAll { (a: Long) =>
    val eins = BigDecimal(a / 60d)
    val zwei = BigDecimal(a) / BigDecimal(60)
    eins == zwei
  }
        
  