package de.codecentric.wittig.scala.scalacheck

import org.scalacheck.Prop.forAll
import org.scalacheck.Properties

object IntTest extends Properties("Int") {

  /**
    * Wird fehlschlagen bei -2147483648
    */
  property("abs") = forAll { i: Int =>
    Math.abs(i) >= 0
  }

}
