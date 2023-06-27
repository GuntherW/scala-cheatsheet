package de.codecentric.wittig.scala.flakyWithRetry

import org.scalatest.{Canceled, Failed, Outcome, Retries}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.tagobjects.Retryable

import scala.util.Random

// https://yadukrishnan.live/mastering-scalatest-exploring-tagging-retry-runner-and-more
class FlakyTest extends AnyFunSuite with Retries:

  private val maxRetryCount = 10

  test("flaky test", Retryable) {
    println("flaky")
    assert(Random.nextBoolean())
  }

  test("good") {
    println("good")
    assert(true)
  }

  override def withFixture(test: NoArgTest): Outcome =
    if (isRetryable(test))
      withRetry(withFixture(test, maxRetryCount))
    else
      super.withFixture(test)

  def withFixture(test: NoArgTest, count: Int): Outcome =
    super.withFixture(test) match
      case Failed(_) | Canceled(_) =>
        if (count == 1) super.withFixture(test)
        else withFixture(test, count - 1)
      case other                   => other
