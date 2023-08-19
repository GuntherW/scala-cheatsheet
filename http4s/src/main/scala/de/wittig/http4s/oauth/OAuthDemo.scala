package de.wittig.http4s.oauth

import cats.effect.*
import ciris.*
import ciris.circe.*
import com.comcast.ip4s.{ipv4, port}
import io.circe.*
import io.circe.parser.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.QueryParamDecoderMatcher
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.{Accept, Authorization}
import org.http4s.implicits.*

import java.nio.file.Paths

object OAuthDemo extends IOApp.Simple {
  private val dsl = Http4sDsl[IO]
  import dsl.*

  def routes(config: AppConfig): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ GET -> Root / "home"                                     => StaticFile.fromString("http4s/src/main/resources/index.html", Some(req)).getOrElseF(NotFound())
    case GET -> Root / "callback" :? GithubTokenQueryParamMatcher(code) => getOAuthResult(code, config).flatMap(result => Ok(result))
  }

  def fetchToken(code: String, config: AppConfig): IO[Option[String]] = {
    val urlForm = UrlForm("client_id" -> config.key, "client_secret" -> config.secret.value, "code" -> code)
    val request = Request[IO](
      Method.POST,
      uri"https://github.com/login/oauth/access_token",
      headers = Headers(Accept(MediaType.application.json))
    ).withEntity(urlForm)
    EmberClientBuilder.default[IO].build
      .use(_.expect[String](request))
      .map(decode[GithubTokenResponse])
      .map {
        case Left(error)     => None
        case Right(response) => Some(response.accessToken)
      }
  }

  def fetchUserInfo(token: String): IO[String] = {
    val req = Request[IO](
      Method.GET,
      uri"https://api.github.com/user/emails",
      headers = Headers(
        Accept(MediaType.application.json),
        Authorization(Credentials.Token(AuthScheme.Bearer, token))
      )
    )

    EmberClientBuilder
      .default[IO].build
      .use(_.expect[String](req))
  }

  def getOAuthResult(code: String, config: AppConfig): IO[String] = for {
    token  <- fetchToken(code, config)
    result <- token match {
                case Some(token) => fetchUserInfo(token)
                case None        => IO.pure("Error")
              }
  } yield result

  override def run: IO[Unit] = for {
    config <- AppConfig.conf.load[IO]
    server <- EmberServerBuilder.default[IO]
                .withHost(ipv4"0.0.0.0")
                .withPort(port"8080")
                .withHttpApp(routes(config).orNotFound)
                .build
                .use(_ => IO.println("Server started at http://localhost:8080/home") *> IO.never)
  } yield ()
}

object GithubTokenQueryParamMatcher extends QueryParamDecoderMatcher[String]("code")

case class AppConfig(key: String, secret: Secret[String])
object AppConfig {
  val conf: ConfigValue[Effect, AppConfig] = file(Paths.get("http4s/src/main/resources/appConfig.json")).as[AppConfig]

  given appDecoder: Decoder[AppConfig] = Decoder.instance { h =>
    for {
      key    <- h.get[String]("key")
      secret <- h.get[String]("secret")
    } yield AppConfig(key, Secret(secret))
  }

  given appConfigDecoder: ConfigDecoder[String, AppConfig] = circeConfigDecoder("AppConfig")
}

case class GithubTokenResponse(accessToken: String, scope: String, tokenType: String)
object GithubTokenResponse {
  given decoder: Decoder[GithubTokenResponse] = Decoder.instance { h =>
    for {
      accessToken <- h.get[String]("access_token")
      scope       <- h.get[String]("scope")
      tokenType   <- h.get[String]("token_type")
    } yield GithubTokenResponse(accessToken, scope, tokenType)
  }
}
