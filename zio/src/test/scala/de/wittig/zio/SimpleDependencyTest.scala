package de.wittig.zio

import zio.test.*
import zio.*
import zio.test.TestAspect.*

object SimpleDependencyTest extends ZIOSpecDefault:
  def spec = suite("dependency")(
    test("provide") {
      val aZio: ZIO[Int, Nothing, Int] = ZIO.succeed(42)
      assertZIO(aZio)(Assertion.equalTo(42))
    }.provide(ZLayer.succeed(42)),
    test("console") {
      val consoleTest: Task[Vector[String]] =
        for
          _      <- TestConsole.feedLines("Gunther")
          _      <- DummyConsoleApplication.welcome()
          output <- TestConsole.output
        yield output.map(_.trim)

      assertZIO(consoleTest)(Assertion.hasSameElements(List("Please enter your name...", "", "Welcome, Gunther")))
    },
    test("clock") {
      val parallelEffect =
        for
          fiber  <- ZIO.sleep(5.minutes).timeout(1.minutes).fork
          _      <- TestClock.adjust(1.minute)
          result <- fiber.join
        yield result
      assertZIO(parallelEffect)(Assertion.isNone)
    },
    test("random") {
      val effect =
        for
          _     <- TestRandom.feedInts(3, 2, 4)
          value <- Random.nextInt
        yield value

      assertZIO(effect)(Assertion.equalTo(3))
    },
    test("aspects") {
      val effect =
        for
          fib <- (ZIO.sleep(2.seconds) *> ZIO.succeed(42)).fork
          _   <- TestClock.adjust(3.seconds)
          v   <- fib.join
        yield v
      assertZIO(effect)(Assertion.equalTo(42))
    } @@ timeout(10.seconds)
    // Aspects:
    // - timeout(duration)
    // - eventually - retries until successful
    // - nonFlaky(n) - repeats n times, stops at first failure
    // - repeats(n) - same
    // - retries(n) - retries n times, stops at first success
    // - debug - prints everything to console
    // - silent - prints nothing
    // - diagnose(duration) - infos, if fiber is interrupted
    // - parallel/sequential (for suites)
    // - ignore
    // - success - (for suites) will fail all ignored tests
    // - timed - measure execution time
    // - before/beforeAll after/afterAll
  ) @@ parallel @@ timed

object DummyConsoleApplication:
  def welcome(): Task[Unit] =
    for
      _    <- Console.printLine("Please enter your name...")
      name <- Console.readLine("")
      _    <- Console.printLine(s"Welcome, $name")
    yield ()
