package de.wittig.cucumber

import io.cucumber.scala.{DE, EN, ScalaDsl, Scenario}
import org.junit.Assert.*

class Di1StepDefinitions extends ScalaDsl with DE {

  var a: Int = _
  var b: Int = _

  Angenommen("""Ich habe die Werte {} und {}""") { (pa: Int, pb: Int) =>
    a = pa
    b = pb
  }
}

class Di2StepDefinitions(di1: Di1StepDefinitions) extends ScalaDsl with DE {

  var addition: Int = _

  Wenn("""ich diese Werte addiere""") { () =>
    addition = Calculator.add(di1.a, di1.b)
  }
}

class Di3StepDefinitions(di2: Di2StepDefinitions) extends ScalaDsl with DE {

  Dann("""sei das Ergebnis {}""") { (c: Int) =>
    assertEquals(c, di2.addition)
  }
}
