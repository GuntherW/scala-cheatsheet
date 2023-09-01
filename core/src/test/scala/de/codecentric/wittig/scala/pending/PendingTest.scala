package de.codecentric.wittig.scala.pending

import org.scalatest.funsuite.AnyFunSuite

class PendingTest extends AnyFunSuite:

  // Wird nicht ausgeführt
  ignore("Ignored 1"):
    assert(true)

  // Wird nicht ausgeführt
  test("Pending 1"):
    pending
    assert(true)

  // Wird ausgeführt, muß aber fehlschlagen
  test("Pending 2"):
    pendingUntilFixed:
      assert(false)
