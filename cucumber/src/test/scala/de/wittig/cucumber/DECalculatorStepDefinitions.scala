package de.wittig.cucumber

import io.cucumber.scala.{DE, EN, ScalaDsl, Scenario}
import org.junit.Assert.*

class DECalculatorStepDefinitions extends ScalaDsl, DE:

  var added: Int = 0

  Wenn("""Ich addiere {} und {}""") { (a: Int, b: Int) =>
    added = Calculator.add(a, b)
  }

  Dann("ist das Ergebnis {}") { (c: Int) =>
    assertEquals(c, added)
  }
