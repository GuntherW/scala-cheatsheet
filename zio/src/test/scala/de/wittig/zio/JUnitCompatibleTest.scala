package de.wittig.zio

import zio.*
import zio.test.*
import zio.test.junit.JUnitRunnableSpec

class JUnitCompatibleTest extends JUnitRunnableSpec {
  def spec = suite("some suite")(
    test("passing test") {
      assert(1)(Assertion.equalTo(1))
    },
    test("passing test assertTrue") {
      assertTrue(1 == 1)
    }
  )
}
