package de.wittig.http4s.session

import cats.data.*
import cats.effect.*
import com.comcast.ip4s.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.*
import org.http4s.headers.Cookie
import org.http4s.implicits.*
import org.http4s.server.*
import org.http4s.server.middleware.authentication.DigestAuth
import org.http4s.server.middleware.authentication.DigestAuth.Md5HashedAuthStore

import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.Base64
import scala.util.*

object Session extends IOApp:

  case class User(id: Long, name: String)

  val today: String                                = LocalDateTime.now().toString
  def setToken(user: String, date: String): String = Base64.getEncoder.encodeToString(s"${user}:{$today}".getBytes(StandardCharsets.UTF_8))
  def getUser(token: String): Try[String]          = Try(new String(Base64.getDecoder.decode(token)).split(":")(0))

  val ha1: IO[String] = Md5HashedAuthStore.precomputeHash[IO]("username", "http://localhost:8080/welcome", "password")

  val funcPass: String => IO[Option[(User, String)]] = {
    case "username" => ha1.flatMap(hash => IO(Some(User(1, "username"), hash)))
    case _          => IO(None)
  }

  val authedRoutes: AuthedRoutes[User, IO] =
    AuthedRoutes.of {
      case GET -> Root / "welcome" as user => Ok(s"Welcome, ${user.name}").map(_.addCookie(ResponseCookie("sessioncookie", setToken(user.name, today), maxAge = Some(86400))))
    }

  val cookieAccessRoutes = HttpRoutes.of[IO] {
    case GET -> Root / "statement" / user => Ok(s"Welcome back $user, Financial statement processing...")
    case GET -> Root / "logout"           => Ok("Logging out...").map(_.removeCookie("sessioncookie"))
  }

  def modifyPath(user: String): Path = Uri.Path.unsafeFromString(s"/statement/$user")

  def cookieCheckerService(service: HttpRoutes[IO]): HttpRoutes[IO] = Kleisli { req =>
    val authHeader: Option[Cookie] = req.headers.get[Cookie]
    OptionT.liftF(authHeader match
      case Some(cookie) =>
        cookie.values.toList.find(_.name == "sessioncookie") match
          case Some(token) =>
            getUser(token.content) match
              case Success(user) => service.orNotFound.run(req.withPathInfo(Uri.Path.unsafeFromString(s"/statement/$user")))
              case Failure(_)    => Ok("Invalid token")
          case None        => Ok("No token")
      case None         => Ok("No cookies"))
  }

  override def run(args: List[String]): IO[ExitCode] =
    for {
      middleware <- DigestAuth.applyF[IO, User]("http://localhost:8080/welcome", Md5HashedAuthStore(funcPass))
      digestService = middleware(authedRoutes)
      serviceRouter = Router(
        "/login" -> digestService,
        "/"      -> cookieCheckerService(cookieAccessRoutes)
      )
      server = EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"9083")
        .withHttpApp(serviceRouter.orNotFound)
        .build
      exitCode <- server.use(_ => IO.never).as(ExitCode.Success)
    } yield exitCode
