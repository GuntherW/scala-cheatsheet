package de.wittig.scala.scalacheck

import org.scalacheck.Prop._
import org.scalacheck._

object WithConstraintsTest extends Properties("Int") {

  /** Würde ohne Constraint fehlschlagen bei Int.MinValue
    */
  property("abs >= 0") = forAll { (i: Int) =>
    i > Int.MinValue ==> // Constraint
      Math.abs(i) >= 0
  }
}
