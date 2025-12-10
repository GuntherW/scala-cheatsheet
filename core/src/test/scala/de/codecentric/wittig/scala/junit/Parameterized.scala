package de.codecentric.wittig.scala.junit

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

// Lediglich Junit-Jupiter als Abhängigkeit aufnehmen.
// Für SBT auch noch jupiter-interface als Abhängigkeit aufnehmen.
class Parameterized {

  @Test
  def test(): Unit =
    assert(1 == 1)

  @ParameterizedTest
  @ValueSource(ints = Array(1, 3, 5, -3, 15, Int.MaxValue))
  def parameter(number: Int): Unit =
    assert(Numbers.isOdd(number))
}

object Numbers {
  def isOdd(number: Int): Boolean = number % 2 != 0
}
