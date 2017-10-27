package de.codecentric.wittig.scala.scalacheck

import org.scalacheck.Arbitrary._
import org.scalacheck.Prop._
import org.scalacheck._

object IntTest extends Properties("Int") {

  /**
    * Würde ohne Constraint fehlschlagen bei Int.MinValue
    */
  property("abs >= 0") = forAll { i: Int =>
    i > Int.MinValue ==> // Constraint
      Math.abs(i) >= 0
  }

}
