package de.wittig.zio.rockthejvm.asynchronous

import de.wittig.zio.rockthejvm.util.debugThread
import zio.*

import java.util.concurrent.Executors

object AsynchronousEffects extends ZIOAppDefault {

  // Callback-based

  object LoginService {
    case class AuthError(message: String)
    case class UserProfile(email: String, name: String)

    // thread pool
    val executor = Executors.newFixedThreadPool(8)

    // database
    val passwd   = Map("gunther@wittig.de" -> "passwd")
    val database = Map("gunther@wittig.de" -> "Gun")

    def login(email: String, password: String)(onSuccess: UserProfile => Unit, onFailure: AuthError => Unit) =
      executor.execute { () =>
        println(s"[${Thread.currentThread().getName}] Attempting login for $email")
        passwd.get(email) match
          case Some(`password`) => onSuccess(UserProfile(email, database(email)))
          case Some(_)          => onFailure(AuthError("Incorrect password."))
          case None             => onFailure(AuthError(s"User $email doesn't exist"))

      }
  }

  def loginAsZIO(id: String, pw: String): ZIO[Any, LoginService.AuthError, LoginService.UserProfile] =
    ZIO.async[Any, LoginService.AuthError, LoginService.UserProfile] { cb =>
      LoginService.login(id, pw)(
        profile => cb(ZIO.succeed(profile)),
        error => cb(ZIO.fail(error))
      )
    }

  val loginProgramm =
    for
      email   <- Console.readLine("Email: ")
      pw      <- Console.readLine("Password: ")
      profile <- loginAsZIO(email, pw).debugThread
      _       <- Console.printLine(s"Welcome ${profile.name}")
    yield ()

  def run = loginProgramm
}
