package de.wittig.zio.rockthejvm.effects

import de.wittig.zio.rockthejvm.effects.ZIOErrorHandling.{QueryError, UserProfile}
import zio.*

import java.io.IOException
import java.net.NoRouteToHostException
import scala.util.Try

object ZIOErrorHandling extends ZIOAppDefault:

  // ZIOs can fail
  val aFailed                = ZIO.fail("Boom")
  val aFailedWithThrowable   = ZIO.fail(new RuntimeException("Boom"))
  val aFailedWithDescription = aFailedWithThrowable.mapError(_.getMessage)

  // attempt: run an effect that might throw an exception
  val badZIO = ZIO.succeed {
    println("somthing")
    val string: String = null
    string.length
  }

  // use attempt if you are ever unsure wether your code might throw
  val betterZIO = ZIO.attempt {
    println("somthing")
    val string: String = null
    string.length
  }

  // effectrully catch errors
  val catchError           = betterZIO.catchAll(e => ZIO.succeed(s"Returning a different value because $e"))
  val catchSelectiveErrors = betterZIO.catchSome {
    case e: RuntimeException => ZIO.succeed(s"Ignoring runter exceptions: $e")
    case _                   => ZIO.succeed("Ignoring everything else")
  }

  // chain effects
  val aBetterAttempt = betterZIO.orElse(ZIO.succeed(56))
  // fold: handel both success and failure
  val handleBoth     = betterZIO.fold(ex => s"Something bad happened $ex", value => s"Length of the string was $value")
  // effectful fold
  val handleBoth_v2  = betterZIO.foldZIO(
    ex => ZIO.succeed(s"Something bad happened $ex"),
    value => ZIO.succeed(s"Length of the string was $value")
  )

  // Conversions between Option/Try/Either to ZIO
  val aTryToZIO    = ZIO.fromTry(Try(42 / 0)) // can fail with Throwable
  val aEitherToZIO = ZIO.fromEither(Right(42))
  // Either in value channel
  val eitherZIO    = betterZIO.either
  val betterZIO2   = eitherZIO.absolve

  val aOptionToZIO = ZIO.fromOption(Some(42))
  val optionZIO    = betterZIO.option // Option in value channel

  // Errors = failures present in the ZIO typ signature ("checked exceptions")
  // Defects = failures that are unrecoverable, unforeseen, NOT present in the ZIO type signature

  // ZIO[R,E,A] can finish with
  // - Success[A]
  // - Cause[E]
  //   - Fail[E] containing the error
  //   - Die(t: Throwable) which was unforeseen

  val divisionByZIO: UIO[Int] = ZIO.succeed(1 / 0)

  val failedInt: ZIO[Any, String, Int]                   = ZIO.fail("I failed")
  val failureCausedExposed: ZIO[Any, Cause[String], Int] = failedInt.sandbox
  val failureCauseHidden: ZIO[Any, String, Int]          = failureCausedExposed.unsandbox

  val foldedWithCause    = divisionByZIO.foldCause(
    cause => s"this failed with ${cause.defects}",
    value => s"this succeeded with $value"
  )
  val foldedWithCause_v2 = divisionByZIO.foldCauseZIO(
    cause => ZIO.succeed(s"this failed with ${cause.defects}"),
    value => ZIO.succeed(s"this succeeded with $value")
  )

  /*
  Good Practice:
    - at a lower level, your "errors" should be treated
    - at a higher level, you should hide "errors" and assume they are unrecoverable
   */

  /* Example: Errors to Defects */
  def callHttpEndpoint(url: String): ZIO[Any, IOException, String] = ZIO.fail(new IOException("no internet"))
  val endpointCallWithDefects: ZIO[Any, Nothing, String]           = callHttpEndpoint("https://www.codecentric.de").orDie // all errors are now defects

  // refining the error channel
  def callHTTPEndpointWideError(url: String): ZIO[Any, Exception, String] =
    ZIO.fail(new IOException("not internet"))

  def callHttpEndpoint_v2(url: String): ZIO[Any, IOException, String] =
    callHTTPEndpointWideError(url).refineOrDie[IOException] {
      case e: IOException            => e
      case e: NoRouteToHostException => new IOException("no route")
    }

  /* Example: Defects to Errors */
  val endpointCallWithError = endpointCallWithDefects.unrefine {
    case e => e.getMessage
  }

  /*
    Combine effects with different errors
   */

  case class IndexError(message: String)
  case class DbError(message: String)
  val callApi: ZIO[Any, IndexError, String] = ZIO.succeed("{json}")
  val queryDb: ZIO[Any, DbError, Int]       = ZIO.succeed(1)

  val combined: ZIO[Any, IndexError | DbError, (String, Int)] =
    for
      page <- callApi
      rows <- queryDb
    yield (page, rows)

  // Exercise
  // 1. make this effect fail with a typed error
  val aBadFailure                                       = ZIO.succeed[Int](throw new RuntimeException("this is bad"))
  val aBadFailureAnswer1: ZIO[Any, Cause[Nothing], Int] = aBadFailure.sandbox
  val aBadFailureAnswer2: ZIO[Any, Throwable, Int]      = aBadFailure.unrefine(e => e)

  // 2. transform a zio into another zio with a narrower exception type
  def ioException[R, A](zio: ZIO[R, Throwable, A]): ZIO[R, IOException, A] = zio.refineOrDie[IOException] {
    case e: IOException => e
  }

  // 3
  def lefta[R, E, A, B](zio: ZIO[R, E, Either[A, B]]): ZIO[R, Either[E, A], B] =
    zio.foldZIO(
      e => ZIO.fail(Left(e)),
      either =>
        either match
          case Left(a)  => ZIO.fail(Right(a))
          case Right(b) => ZIO.succeed(b)
    )

  // 3
  def leftb[R, E, A, B](zio: ZIO[R, E, Either[A, B]]): ZIO[R, E | A, B] =
    zio.flatMap {
      case Left(value)  => ZIO.fail(value)
      case Right(value) => ZIO.succeed(value)
    }

  // 4
  val database = Map(
    "daniel" -> 123,
    "alice"  -> 234
  )
  case class QueryError(reason: String)
  case class UserProfile(name: String, phone: Int)

  def lookupProfile(userId: String): ZIO[Any, QueryError, Option[UserProfile]] =
    if (userId != userId.toLowerCase())
      ZIO.fail(QueryError("userId format invalid"))
    else
      ZIO.succeed(database.get(userId).map(phone => UserProfile(userId, phone)))

  def betterLookupProfile(userId: String): ZIO[Any, Option[QueryError], UserProfile]    =
    lookupProfile(userId).foldZIO(
      e => ZIO.fail(Some(e)),
      profileOption =>
        profileOption match
          case Some(profile) => ZIO.succeed(profile)
          case None          => ZIO.fail(None)
    )
  def betterLookupProfile_v2(userId: String): ZIO[Any, Option[QueryError], UserProfile] =
    lookupProfile(userId).some

  override def run =
    for
      f <- foldedWithCause
      p <- failedInt
      _ <- Console.printLine(f)
      _ <- Console.printLine(p)
    yield ()
