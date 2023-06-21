package de.codecentric.wittig.scala.mockito

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.mockito.stubbing.Answer
import org.scalatest.funsuite.AnyFunSuite

class MockingTest extends AnyFunSuite {

  class MyTestClass() {
    def m1(s: String): String              = s
    def m2(s: String, s2: String*): String = s + s2.mkString
  }

  private val m = mock(classOf[MyTestClass])

  test("mock") {
    when(m.m1(any())).thenReturn("Moin")
    assert(m.m1("k") === "Moin")
  }

  test("mock varargs") {
    when(m.m2(any(), any())).thenReturn("Moin")
    assert(m.m2("eins") === "Moin")
    assert(m.m2("eins", "zwei") === "Moin")
    assert(m.m2("eins", "zwei", "drei") === "Moin")
  }

  test("mock more varargs") {
    when(m.m2(any(), any())).thenAnswer(s => s.getArgument[String](0).reverse)
    assert(m.m2("eins") === "snie")
    assert(m.m2("eins", "zwei") === "snie")
    assert(m.m2("eins", "zwei", "drei") === "snie")
  }

}
