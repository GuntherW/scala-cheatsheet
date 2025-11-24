package de.wittig.cucumber

import io.cucumber.scala.{DE, ScalaDsl}
import org.junit.jupiter.api.Assertions.*

class DECalculatorStepDefinitions extends ScalaDsl, DE:

  var added: Int = 0

  Wenn("""ich {} und {} addiere""") { (a: Int, b: Int) =>
    added = Calculator.add(a, b)
  }

  Dann("ist das Ergebnis {}") { (c: Int) =>
    assertEquals(c, added)
  }
