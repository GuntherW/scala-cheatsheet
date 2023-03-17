package de.wittig.zio

import zio.test.*
import zio.*

object PropertyBasedTest extends ZIOSpecDefault:
  def spec = test("property based") {
    check(Gen.int, Gen.int, Gen.int) { (a, b, c) =>
      assertTrue((a + b) + c == a + (b + c))
    }
  }
