package de.codecentric.wittig.scala.mockito

import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, verify, when}
import org.mockito.stubbing.Answer
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfterEach
import scala.jdk.CollectionConverters.*

class MockingTest extends AnyFunSuite, BeforeAndAfterEach:

  class MyTestClass:
    def m1(s: String): String              = s
    def m2(s: String, s2: String*): String = s + s2.mkString
    def m3(s2: String*): String            = s2.mkString

  private val m = mock(classOf[MyTestClass])

  override def beforeEach(): Unit =
    super.beforeEach()
    reset(m)

  test("mock"):
    when(m.m1(any)).thenReturn("Moin")
    assert(m.m1("k") === "Moin")
    verify(m).m1("k")

  test("mock varargs"):
    when(m.m2(any, any)).thenReturn("Moin")
    assert(m.m2("eins") === "Moin")
    assert(m.m2("eins", "zwei") === "Moin")
    assert(m.m2("eins", "zwei", "drei") === "Moin")

  test("mock more varargs"):
    when(m.m2(any, any)).thenAnswer(s => s.getArgument[String](0).reverse)
    assert(m.m2("eins") === "snie")
    assert(m.m2("eins", "zwei") === "snie")
    assert(m.m2("eins", "zwei", "drei") === "snie")

    verify(m).m2("eins")
    verify(m).m2("eins", "zwei")
    verify(m).m2("eins", "zwei", "drei")

  test("capturing"):
    m.m1("eins")
    verify(m).m1("eins")

    val varargs: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    verify(m).m1(varargs.capture())
    assert(varargs.getValue == "eins")

  test("capturing varargs"):
    m.m3("eins", "zwei", "drei")
    val varargs: ArgumentCaptor[Seq[String]] = ArgumentCaptor.forClass(classOf[Seq[String]])
    verify(m).m3(varargs.capture()*)

    val captured = varargs.getValue
    assert(captured == Seq("eins", "zwei", "drei"))
