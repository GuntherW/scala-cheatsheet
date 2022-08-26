package de.wittig.cucumber

import io.cucumber.scala.{EN, ScalaDsl, Scenario}
import org.junit.Assert.*

class CalculatorStepDefinitions extends ScalaDsl with EN:

  var added: Int = 0

  When("""I add {} and {}""") { (a: Int, b: Int) =>
    added = Calculator.add(a, b)
  }

  Then("the result is {}") { (expected: Int) =>
    assertEquals(expected, added)
  }

  Before("not @foo") { (scenario: Scenario) =>
    println(s"Runs before scenarios *not* tagged with @foo (${scenario.getId})")
  }

  Before("@foo") { (scenario: Scenario) =>
    println(s"Runs before scenarios tagged with @foo (${scenario.getId})")
  }
