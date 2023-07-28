package de.wittig.http4s.jwt
import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.*
import com.comcast.ip4s.*
import cats.implicits.*
import dev.profunktor.auth.*
import dev.profunktor.auth.jwt.*
import pdi.jwt.*
import java.time.Instant
import io.circe.*
import io.circe.parser.*

object Jwt extends IOApp:

  private val claim = JwtClaim(
    content = """{"user":"John", "level":"basic"}""",
    expiration = Some(Instant.now.plusSeconds(157784760).getEpochSecond),
    issuedAt = Some(Instant.now.getEpochSecond)
  )

  private val key      = "secretKey"
  private val algo     = JwtAlgorithm.HS256
  private val token    = JwtCirce.encode(claim, key, algo)
  private val database = Map("John" -> AuthUser(123, "JohnDoe"))

  private val authenticate: JwtToken => JwtClaim => IO[Option[AuthUser]] =
    (token: JwtToken) =>
      (claim: JwtClaim) =>
        decode[TokenPayLoad](claim.content) match
          case Right(payload) => IO(database.get(payload.user))
          case Left(_)        => IO(None)

  private val middleware = JwtAuthMiddleware[IO, AuthUser](JwtAuth.hmac(key, algo), authenticate)

  private val authedRoutes: AuthedRoutes[AuthUser, IO] = AuthedRoutes.of {
    case GET -> Root / "welcome" as user => Ok(s"Welcome, ${user.name}")
  }

  private val loginRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "login" => Ok(s"Logged In").map(_.addCookie(ResponseCookie("token", token)))
  }

  private val securedRoutes: HttpRoutes[IO] = middleware(authedRoutes)
  private val service                       = loginRoutes <+> securedRoutes

  private val server = EmberServerBuilder
    .default[IO]
    .withHost(ipv4"0.0.0.0")
    .withPort(port"9084")
    .withHttpApp(service.orNotFound)
    .build

  override def run(args: List[String]): IO[ExitCode] =
    server
      .use(_ => IO.never)
      .as(ExitCode.Success)

case class AuthUser(id: Long, name: String)
case class TokenPayLoad(user: String, level: String)
object TokenPayLoad:
  given decoder: Decoder[TokenPayLoad] = Decoder.instance { h =>
    for
      user  <- h.get[String]("user")
      level <- h.get[String]("level")
    yield TokenPayLoad(user, level)
  }
