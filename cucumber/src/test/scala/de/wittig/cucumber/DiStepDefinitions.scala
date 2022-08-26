package de.wittig.cucumber

import scala.compiletime.uninitialized

import io.cucumber.scala.{DE, EN, ScalaDsl, Scenario}
import org.junit.Assert.*

class Di1StepDefinitions extends ScalaDsl with DE:

  var a: Int = 0
  var b: Int = 0

  Angenommen("""Ich habe die Werte {} und {}""") { (pa: Int, pb: Int) =>
    a = pa
    b = pb
  }

class Di2StepDefinitions(di1: Di1StepDefinitions) extends ScalaDsl with DE:

  var addition: Int = uninitialized

  Wenn("""ich diese Werte addiere""") { () =>
    addition = Calculator.add(di1.a, di1.b)
  }

class Di3StepDefinitions(di2: Di2StepDefinitions) extends ScalaDsl with DE:

  Dann("""sei das Ergebnis {}""") { (c: Int) =>
    assertEquals(c, di2.addition)
  }