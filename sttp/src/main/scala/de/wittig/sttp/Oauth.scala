package de.wittig.sttp

import com.ocadotechnology.sttp.oauth2.{Secret, SttpOauth2ClientCredentialsBackend}
import com.ocadotechnology.sttp.oauth2.common.Scope
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.auto.*
import sttp.client3.*
import sttp.client3.circe.*
import sttp.client3.logging.slf4j.Slf4jLoggingBackend

/** */
object Oauth extends App:

  private val delegateBackend             = Slf4jLoggingBackend(HttpClientSyncBackend(), logRequestBody = true, logResponseBody = true)
  private val scope: Option[Scope]        = Scope.of("scope")
  private val tokenUrl                    = uri"https://api.github.com"
  private val clientId                    = NonEmptyString.unsafeFrom("123456")
  private val clientSecret                = Secret("")
  val backend: SttpBackend[Identity, Any] = SttpOauth2ClientCredentialsBackend[Identity, Any](tokenUrl, clientId, clientSecret)(scope)(delegateBackend)

  private val query = "language:scala"

  private val request = basicRequest
    .get(uri"https://api.github.com/search/repositories?q=$query")
    .response(asJson[GitHubResponse])
    .header("a", "b")

  request
    .send(backend)
    .body
    .foreach(_.items.foreach(println))
