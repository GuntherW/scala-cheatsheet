//> using dep "org.scalameta::munit::1.0.4"
//> using file "SharedCode.scala"

class SharedCodeTests extends munit.FunSuite:
  test("renderText adds header and footer") {
    val result = renderText("Hallo Welt")
    assertEquals(result.split("\n").length, 5)
  }
