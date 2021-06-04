package de.wittig

import monix.eval.Task

import java.nio.file.{Files, Path}
import scala.concurrent.Future
import scala.util.Properties
import munit._
import monix.execution.Scheduler.Implicits.global

//@IgnoreSuite
class FunTest extends FunSuite {

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

  test("Futures should be 1") {
    val f1 = Future(1)
    f1.map { f =>
      assertEquals(f, 1)
    }
  }

  test("async") {
    Future {
      println("Hello Internet!")
    }
  }

  test("failing test".fail) {
    println("failing")
    assertEquals(1, 2)
  }

  test("with clues".ignore) {
    val a = 3
    val b = 4
    if (a == 3) // Test direkt fehlschlagen lassen mit Hinweis
      fail("nicht gut", clues(a + b))
    assertEquals(a + b, 7)
  }

  test("exception") {
    intercept[java.lang.IllegalArgumentException] {
      throw new IllegalArgumentException("Mit Message")
    }
  }

  test("exception mit message") {
    interceptMessage[java.lang.IllegalArgumentException]("Mit Message") {
      throw new IllegalArgumentException("Mit Message")
    }
  }

  // Mit ValueTransformers für z.B. Monix.Task
  override def munitValueTransforms: List[ValueTransform] =
    super.munitValueTransforms :+ new ValueTransform(
      "IO",
      { case task: Task[_] => task.runToFuture } // transform in Future
    )

  test("task") {
    val task = Task(1)
    task.map { t1 =>
      assertEquals(t1, 1)
    }
  }

  // Mit TestTransformers, um die Tests selbst anzupassen
  // Hier: Überschreiben aller Testnamen mit der Scalaversion als Suffix
  override def munitTestTransforms: List[TestTransform] =
    super.munitTestTransforms :+ new TestTransform(
      "scala-version",
      test => test.withName(s"${test.name}-${Properties.versionNumberString}")
    )

  // Deklarieren einer Fixture
  private def file(path: String = "tmp") = FunFixture[Path](
    setup = test => Files.createTempFile(path, test.name),
    teardown = file => Files.deleteIfExists(file) // Always gets called, even if test failed.
  )

  // Benutzung einer Fixture
  file().test("with one fixture") { file =>
    assert(Files.isRegularFile(file), s"Files.isRegularFile($file)")
  }

  // Kombinieren von Fixture
  private val twoFiles = FunFixture.map2(file("tmp1"), file("tmp2"))

  twoFiles.test("with two fixtures") { case (file1, file2) =>
    assert(Files.isRegularFile(file1), s"Files.isRegularFile($file1)")
    assert(Files.isRegularFile(file2), s"Files.isRegularFile($file2)")
  }
}
