package de.wittig

import munit.ScalaCheckSuite
import org.scalacheck.Prop.*

class CheckTest extends ScalaCheckSuite:

  property("addition is commutative") {
    forAll { (n1: Int, n2: Int) =>
      n1 + n2 == n2 + n1
    }
  }

  property("0 is the identity of addition") {
    forAll { (n: Int) =>
      n + 0 == n
    }
  }
