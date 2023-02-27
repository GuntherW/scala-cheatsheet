package de.wittig.zio.rockthejvm.effects

import zio.*

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

  override def run = ???
