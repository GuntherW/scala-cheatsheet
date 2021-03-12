package munit

import java.nio.file.{Files, Path}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Properties

//@munit.IgnoreSuite
class FirstTest extends munit.FunSuite {
  case class Person(name: String, color: String)

  test("should run on windows only") {
    assume(Properties.isWin, "this test runs only on Linux") // ignore test, if running on windoof
    assert(true)
  }

  test("Actionable errors -- bad way".ignore) {
    val realPerson   = Person("Hans", "red")
    val wronglPerson = Person("Hans", "black")
    assert(realPerson == wronglPerson)
  }

  test("Actionable errors -- good way".ignore) {
    val realPerson   = Person("Hans", "red")
    val wronglPerson = Person("Hans", "black")
    assertEquals(realPerson, wronglPerson)
  }

  test("Futures should be ") {
    val f1 = Future(1)
    f1.map(f => assertEquals(f, 1))
  }

  test("async") {
    Future {
      println("Hello Internet!")
    }
  }

  test("failing test".fail) {
    assertEquals(1, 2)
  }

  test("exception") {
    intercept[java.lang.IllegalArgumentException] {
      throw new IllegalArgumentException
    }
  }

  test("exception mit message") {
    interceptMessage[java.lang.IllegalArgumentException]("Mit Message") {
      throw new IllegalArgumentException("Mit Message")
    }
  }

  // Deklarieren einer Fixture
  private val files = FunFixture[Path](
    setup = { test =>
      Files.createTempFile("tmp", test.name)
    },
    teardown = { file =>
      // Always gets called, even if test failed.
      Files.deleteIfExists(file)
    }
  )

  // Benutzung einer Fixture
  files.test("basic") { file =>
    assert(Files.isRegularFile(file), s"Files.isRegularFile($file)")
  }
}
